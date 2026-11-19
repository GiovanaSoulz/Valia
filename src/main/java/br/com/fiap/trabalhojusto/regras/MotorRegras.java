package br.com.fiap.trabalhojusto.regras;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.TipoIrregularidade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Motor determinístico de regras trabalhistas. Não usa IA nem framework: dado o mesmo
 * input produz sempre o mesmo resultado (auditável e testável).
 * Os resultados são sinais de POSSÍVEIS irregularidades e valores de REFERÊNCIA.
 */
@Component
public class MotorRegras {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final BigDecimal SEMANAL_LEGAL_HORAS = BigDecimal.valueOf(44);
    private static final BigDecimal CENTAVO = new BigDecimal("0.01");

    public record DadosVinculo(TipoContratacao tipo, BigDecimal salarioMensal, int jornadaSemanalHoras,
                               LocalDate dataInicio) {
    }

    public record Jornada(LocalDate data, LocalTime entrada, LocalTime saida, int intervaloMinutos) {
    }

    public record PagamentoDado(LocalDate competencia, BigDecimal valorPago) {
    }

    public record Achado(TipoIrregularidade tipo, LocalDate referencia, String descricao, BigDecimal valorEstimado) {
    }

    public record Resultado(String versaoRegras, List<Achado> achados, BigDecimal diferencaTotalEstimada) {
    }

    private static final class Mes {
        long extraMinutos;
        BigDecimal valorExtras = BigDecimal.ZERO;
        BigDecimal valorIntervalo = BigDecimal.ZERO;
        boolean temJornada;
    }

    public Resultado analisar(DadosVinculo v, List<Jornada> jornadas, List<PagamentoDado> pagamentos,
                              LocalDate inicio, LocalDate fim, LocalDate hoje) {
        List<Achado> achados = new ArrayList<>();
        Map<YearMonth, Mes> meses = new TreeMap<>();

        List<Jornada> ordenadas = jornadas.stream()
                .filter(j -> !j.data().isBefore(inicio) && !j.data().isAfter(fim))
                .sorted(Comparator.comparing(Jornada::data))
                .toList();

        // 1) Salário contratado x salário mínimo proporcional à jornada
        VersaoRegras regraFim = CatalogoRegras.vigenteEm(fim);
        BigDecimal minimoProporcional = minimoProporcional(v, regraFim);
        if (v.salarioMensal().compareTo(minimoProporcional) < 0) {
            achados.add(new Achado(TipoIrregularidade.SALARIO_ABAIXO_DO_MINIMO, inicio,
                    "O salário contratado (" + moeda(v.salarioMensal()) + ") está abaixo do mínimo nacional proporcional à jornada de "
                            + v.jornadaSemanalHoras() + "h semanais (" + moeda(minimoProporcional) + ", regras "
                            + regraFim.versao() + "). A diferença entra no cálculo dos pagamentos.",
                    BigDecimal.ZERO));
        }

        // 2) Análise dia a dia
        Map<LocalDate, Long> trabalhadoPorDia = new HashMap<>();
        Map<LocalDate, Long> extraPorDia = new HashMap<>();
        LocalDateTime fimAnterior = null;

        for (Jornada j : ordenadas) {
            VersaoRegras r = CatalogoRegras.vigenteEm(j.data());
            LocalDateTime ini = LocalDateTime.of(j.data(), j.entrada());
            LocalDateTime fm = LocalDateTime.of(j.data(), j.saida());
            if (!fm.isAfter(ini)) {
                fm = fm.plusDays(1); // jornada que atravessa a meia-noite
            }
            long presenca = Duration.between(ini, fm).toMinutes();
            long trabalhado = Math.max(0, presenca - j.intervaloMinutos());
            BigDecimal valorHora = valorHora(v, r);
            Mes mes = meses.computeIfAbsent(YearMonth.from(j.data()), k -> new Mes());
            mes.temJornada = true;
            trabalhadoPorDia.put(j.data(), trabalhado);

            // descanso entre jornadas
            if (fimAnterior != null) {
                long descanso = Duration.between(fimAnterior, ini).toMinutes();
                if (descanso < r.interjornadaMinimaMinutos()) {
                    achados.add(new Achado(TipoIrregularidade.INTERVALO_ENTRE_JORNADAS, j.data(),
                            "Descanso de " + hm(Math.max(0, descanso)) + " entre a jornada anterior e a de "
                                    + j.data().format(DATA) + " (mínimo de " + hm(r.interjornadaMinimaMinutos()) + ").",
                            BigDecimal.ZERO));
                }
            }
            fimAnterior = fm;

            // intervalo para repouso/alimentação
            if (trabalhado > r.limiteJornadaParaIntervaloMinutos() && j.intervaloMinutos() < r.intervaloMinimoMinutos()) {
                long falta = r.intervaloMinimoMinutos() - j.intervaloMinutos();
                BigDecimal ref = valorMinutos(falta, valorHora, r);
                mes.valorIntervalo = mes.valorIntervalo.add(ref);
                String obs = v.tipo() == TipoContratacao.DOMESTICO
                        ? " Para trabalho doméstico o intervalo pode ser reduzido a 30 min mediante acordo escrito."
                        : "";
                achados.add(new Achado(TipoIrregularidade.INTERVALO_INSUFICIENTE, j.data(),
                        "Em " + j.data().format(DATA) + " o intervalo foi de " + j.intervaloMinutos() + " min em jornada acima de 6h (mínimo de "
                                + r.intervaloMinimoMinutos() + " min). Valor de referência do tempo suprimido: " + moeda(money(ref)) + "." + obs,
                        BigDecimal.ZERO));
            } else if (trabalhado > 240 && trabalhado <= r.limiteJornadaParaIntervaloMinutos() && j.intervaloMinutos() < 15) {
                achados.add(new Achado(TipoIrregularidade.INTERVALO_INSUFICIENTE, j.data(),
                        "Em " + j.data().format(DATA) + " o intervalo foi de " + j.intervaloMinutos()
                                + " min em jornada entre 4h e 6h (mínimo de 15 min).",
                        BigDecimal.ZERO));
            }

            // horas extras diárias
            long extra = Math.max(0, trabalhado - r.jornadaDiariaMaximaMinutos());
            extraPorDia.put(j.data(), extra);
            if (extra > 0) {
                mes.extraMinutos += extra;
                mes.valorExtras = mes.valorExtras.add(valorMinutos(extra, valorHora, r));
            }
            if (extra > r.limiteExtrasDiariasMinutos()) {
                achados.add(new Achado(TipoIrregularidade.JORNADA_EXCEDE_LIMITE, j.data(),
                        "Em " + j.data().format(DATA) + " foram trabalhadas " + hm(trabalhado) + ", com " + hm(extra)
                                + " de horas extras (limite de " + hm(r.limiteExtrasDiariasMinutos()) + " extras por dia).",
                        BigDecimal.ZERO));
            }
        }

        // 3) Excesso semanal (acima de 44h) ainda não contado como extra diária
        Map<LocalDate, List<LocalDate>> semanas = new TreeMap<>();
        for (Jornada j : ordenadas) {
            LocalDate segunda = j.data().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            semanas.computeIfAbsent(segunda, k -> new ArrayList<>()).add(j.data());
        }
        for (List<LocalDate> dias : semanas.values()) {
            LocalDate ultimo = dias.get(dias.size() - 1);
            VersaoRegras r = CatalogoRegras.vigenteEm(ultimo);
            long totalSemana = dias.stream().mapToLong(trabalhadoPorDia::get).sum();
            long extrasDiarias = dias.stream().mapToLong(extraPorDia::get).sum();
            long extraSemanal = Math.max(0, (totalSemana - r.jornadaSemanalMaximaMinutos()) - extrasDiarias);
            if (extraSemanal > 0) {
                Mes mes = meses.get(YearMonth.from(ultimo));
                mes.extraMinutos += extraSemanal;
                mes.valorExtras = mes.valorExtras.add(valorMinutos(extraSemanal, valorHora(v, r), r));
            }
        }

        // 4) Resumo de horas extras apuradas por competência
        for (Map.Entry<YearMonth, Mes> e : meses.entrySet()) {
            Mes m = e.getValue();
            if (m.extraMinutos > 0) {
                VersaoRegras r = CatalogoRegras.vigenteEm(e.getKey().atDay(1));
                achados.add(new Achado(TipoIrregularidade.HORAS_EXTRAS_APURADAS, e.getKey().atDay(1),
                        "Foram apuradas " + hm(m.extraMinutos) + " de horas extras em " + e.getKey().format(MES)
                                + ". Valor de referência com adicional mínimo de " + r.adicionalHoraExtra().movePointRight(2).intValue()
                                + "%: " + moeda(money(m.valorExtras)) + ", salvo compensação regular (banco de horas ou acordo).",
                        BigDecimal.ZERO));
            }
        }

        // 5) Pagamentos x valor esperado por competência
        Map<YearMonth, BigDecimal> pagoPorMes = new HashMap<>();
        for (PagamentoDado p : pagamentos) {
            pagoPorMes.merge(YearMonth.from(p.competencia()), p.valorPago(), BigDecimal::add);
        }
        YearMonth ultimoMes = YearMonth.from(fim);
        YearMonth mesAtual = YearMonth.from(hoje);
        for (YearMonth ym = YearMonth.from(inicio); !ym.isAfter(ultimoMes); ym = ym.plusMonths(1)) {
            Mes m = meses.get(ym);
            BigDecimal pago = pagoPorMes.get(ym);
            VersaoRegras r = CatalogoRegras.vigenteEm(ym.atDay(1));
            BigDecimal fator = fatorProporcional(v, ym);
            if (fator.signum() == 0) {
                continue; // competência anterior ao início do vínculo
            }
            if (pago == null) {
                if (m != null && m.temJornada && ym.isBefore(mesAtual)) {
                    achados.add(new Achado(TipoIrregularidade.PAGAMENTO_NAO_REGISTRADO, ym.atDay(1),
                            "Há jornadas registradas em " + ym.format(MES) + ", mas nenhum pagamento dessa competência foi informado.",
                            BigDecimal.ZERO));
                }
                continue;
            }
            BigDecimal base = baseSalarial(v, r).multiply(fator);
            BigDecimal extras = m == null ? BigDecimal.ZERO : m.valorExtras;
            BigDecimal intervalo = m == null ? BigDecimal.ZERO : m.valorIntervalo;
            BigDecimal esperado = money(base.add(extras).add(intervalo));
            BigDecimal diferenca = esperado.subtract(pago);
            if (diferenca.compareTo(CENTAVO) >= 0) {
                achados.add(new Achado(TipoIrregularidade.PAGAMENTO_ABAIXO_DO_ESPERADO, ym.atDay(1),
                        "Em " + ym.format(MES) + " o valor pago foi " + moeda(pago) + " e o valor de referência é " + moeda(esperado)
                                + " (salário " + moeda(money(base)) + " + horas extras " + moeda(money(extras))
                                + " + intervalo suprimido " + moeda(money(intervalo)) + "). Diferença possível: " + moeda(diferenca) + ".",
                        money(diferenca)));
            }
        }

        achados.sort(Comparator.comparing(Achado::referencia, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Achado::tipo));
        BigDecimal total = achados.stream().map(Achado::valorEstimado).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resultado(regraFim.versao(), List.copyOf(achados), money(total));
    }

    // ---- cálculos auxiliares -------------------------------------------------------------

    private BigDecimal minimoProporcional(DadosVinculo v, VersaoRegras r) {
        return money(r.salarioMinimo()
                .multiply(BigDecimal.valueOf(v.jornadaSemanalHoras()))
                .divide(SEMANAL_LEGAL_HORAS, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal baseSalarial(DadosVinculo v, VersaoRegras r) {
        return v.salarioMensal().max(minimoProporcional(v, r));
    }

    /** Divisor mensal = horas semanais x 5 (44h -> 220, 40h -> 200, 30h -> 150). */
    private BigDecimal valorHora(DadosVinculo v, VersaoRegras r) {
        BigDecimal divisor = BigDecimal.valueOf(v.jornadaSemanalHoras() * 5L);
        return baseSalarial(v, r).divide(divisor, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal valorMinutos(long minutos, BigDecimal valorHora, VersaoRegras r) {
        return valorHora.multiply(BigDecimal.valueOf(minutos))
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.ONE.add(r.adicionalHoraExtra()));
    }

    private BigDecimal fatorProporcional(DadosVinculo v, YearMonth competencia) {
        YearMonth inicioVinculo = YearMonth.from(v.dataInicio());
        if (competencia.isBefore(inicioVinculo)) {
            return BigDecimal.ZERO;
        }
        if (competencia.equals(inicioVinculo) && v.dataInicio().getDayOfMonth() > 1) {
            int diasNoMes = competencia.lengthOfMonth();
            int diasTrabalhados = diasNoMes - v.dataInicio().getDayOfMonth() + 1;
            return BigDecimal.valueOf(diasTrabalhados).divide(BigDecimal.valueOf(diasNoMes), 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE;
    }

    private static BigDecimal money(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String moeda(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    private static String hm(long minutos) {
        long h = minutos / 60;
        long m = minutos % 60;
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
    }
}
