package br.com.fiap.trabalhojusto.regras;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.TipoIrregularidade;
import br.com.fiap.trabalhojusto.regras.MotorRegras.DadosVinculo;
import br.com.fiap.trabalhojusto.regras.MotorRegras.Jornada;
import br.com.fiap.trabalhojusto.regras.MotorRegras.PagamentoDado;
import br.com.fiap.trabalhojusto.regras.MotorRegras.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testes de precisão financeira e de regra do motor (valores conferidos manualmente). */
class MotorRegrasTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 3, 31);
    private static final LocalDate HOJE = LocalDate.of(2026, 4, 15);
    private static final LocalDate COMPETENCIA = LocalDate.of(2026, 3, 1);

    private final MotorRegras motor = new MotorRegras();

    private DadosVinculo vinculo(String salario, int horasSemanais) {
        return new DadosVinculo(TipoContratacao.CLT, new BigDecimal(salario), horasSemanais, LocalDate.of(2025, 1, 1));
    }

    private PagamentoDado pago(String valor) {
        return new PagamentoDado(COMPETENCIA, new BigDecimal(valor));
    }

    private Jornada dia(int diaDoMes, int hEntrada, int hSaida, int intervalo) {
        return new Jornada(LocalDate.of(2026, 3, diaDoMes), LocalTime.of(hEntrada, 0), LocalTime.of(hSaida, 0), intervalo);
    }

    private boolean tem(Resultado r, TipoIrregularidade tipo) {
        return r.achados().stream().anyMatch(a -> a.tipo() == tipo);
    }

    @Test
    void jornadaRegularComPagamentoCorretoNaoGeraIrregularidade() {
        Resultado r = motor.analisar(vinculo("3000.00", 44), List.of(dia(2, 8, 17, 60)), List.of(pago("3000.00")), INICIO, FIM, HOJE);

        assertThat(r.achados()).isEmpty();
        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("0.00");
    }

    @Test
    void umaHoraExtraGeraDiferencaDeReferenciaComAdicionalDeCinquentaPorCento() {
        // salário 2200 / 220 = R$ 10,00 por hora; 1h extra x 1,5 = R$ 15,00
        Resultado r = motor.analisar(vinculo("2200.00", 44), List.of(dia(2, 8, 18, 60)), List.of(pago("2200.00")), INICIO, FIM, HOJE);

        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("15.00");
        assertThat(tem(r, TipoIrregularidade.PAGAMENTO_ABAIXO_DO_ESPERADO)).isTrue();
        assertThat(tem(r, TipoIrregularidade.HORAS_EXTRAS_APURADAS)).isTrue();
    }

    @Test
    void intervaloSuprimidoEhApontadoEValoradoComAdicional() {
        // 9h sem intervalo: 1h extra (R$ 15,00) + 60 min de intervalo suprimido (R$ 15,00)
        Resultado r = motor.analisar(vinculo("2200.00", 44), List.of(dia(2, 8, 17, 0)), List.of(pago("2200.00")), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.INTERVALO_INSUFICIENTE)).isTrue();
        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("30.00");
    }

    @Test
    void salarioAbaixoDoMinimoEhApontadoEDiferencaEntraNoPagamento() {
        // mínimo 2026.1 = R$ 1.621,00; pago R$ 1.000,00 => diferença R$ 621,00
        Resultado r = motor.analisar(vinculo("1000.00", 44), List.of(), List.of(pago("1000.00")), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.SALARIO_ABAIXO_DO_MINIMO)).isTrue();
        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("621.00");
    }

    @Test
    void salarioMinimoEhProporcionalAJornadaContratada() {
        // 22h semanais => metade do mínimo (R$ 810,50); salário de R$ 900,00 está regular
        Resultado r = motor.analisar(vinculo("900.00", 22), List.of(), List.of(pago("900.00")), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.SALARIO_ABAIXO_DO_MINIMO)).isFalse();
        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("0.00");
    }

    @Test
    void descansoMenorQueOnzeHorasEntreJornadasEhApontado() {
        List<Jornada> jornadas = List.of(dia(2, 14, 23, 60), dia(3, 7, 12, 0));
        Resultado r = motor.analisar(vinculo("2200.00", 44), jornadas, List.of(pago("2200.00")), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.INTERVALO_ENTRE_JORNADAS)).isTrue();
    }

    @Test
    void excessoSemanalAcimaDe44hViraHoraExtraMesmoSemExcederOitoHorasPorDia() {
        // seg a sáb, 8h por dia = 48h na semana => 4h extras x R$ 15,00 = R$ 60,00
        List<Jornada> jornadas = new ArrayList<>();
        for (int d = 2; d <= 7; d++) {
            jornadas.add(dia(d, 8, 17, 60));
        }
        Resultado r = motor.analisar(vinculo("2200.00", 44), jornadas, List.of(pago("2200.00")), INICIO, FIM, HOJE);

        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("60.00");
    }

    @Test
    void maisDeDuasHorasExtrasNoDiaExcedeOLimite() {
        // 07:00 às 18:00 com 30 min de intervalo = 10h30 (2h30 extras)
        Resultado r = motor.analisar(vinculo("2200.00", 44),
                List.of(new Jornada(LocalDate.of(2026, 3, 2), LocalTime.of(7, 0), LocalTime.of(18, 0), 30)),
                List.of(pago("2200.00")), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.JORNADA_EXCEDE_LIMITE)).isTrue();
    }

    @Test
    void mesPassadoComJornadasESemPagamentoEhSinalizado() {
        Resultado r = motor.analisar(vinculo("2200.00", 44), List.of(dia(2, 8, 17, 60)), List.of(), INICIO, FIM, HOJE);

        assertThat(tem(r, TipoIrregularidade.PAGAMENTO_NAO_REGISTRADO)).isTrue();
        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("0.00");
    }

    @Test
    void mesEmAndamentoSemPagamentoNaoEhSinalizado() {
        LocalDate hojeNoMesDaAnalise = LocalDate.of(2026, 3, 20);
        Resultado r = motor.analisar(vinculo("2200.00", 44), List.of(dia(2, 8, 17, 60)), List.of(), INICIO, FIM, hojeNoMesDaAnalise);

        assertThat(tem(r, TipoIrregularidade.PAGAMENTO_NAO_REGISTRADO)).isFalse();
    }

    @Test
    void primeiroMesDoVinculoEhProporcionalAosDiasTrabalhados() {
        // início em 16/03: 16 dias de 31 => 2200 x 16/31 = R$ 1.135,48; pago o mês cheio, sem diferença
        DadosVinculo v = new DadosVinculo(TipoContratacao.CLT, new BigDecimal("2200.00"), 44, LocalDate.of(2026, 3, 16));
        Resultado r = motor.analisar(v, List.of(), List.of(pago("1135.48")), INICIO, FIM, HOJE);

        assertThat(r.diferencaTotalEstimada()).isEqualByComparingTo("0.00");
    }

    @Test
    void regrasSaoVersionadasPorDataDeVigencia() {
        assertThat(CatalogoRegras.vigenteEm(LocalDate.of(2025, 6, 1)).versao()).isEqualTo("2025.1");
        assertThat(CatalogoRegras.vigenteEm(LocalDate.of(2026, 6, 1)).versao()).isEqualTo("2026.1");
        assertThat(CatalogoRegras.vigenteEm(LocalDate.of(2020, 1, 1)).versao()).isEqualTo("2024.1");
    }

    @Test
    void resultadoEhDeterministico() {
        List<Jornada> jornadas = List.of(dia(2, 8, 18, 30), dia(3, 8, 19, 0));
        Resultado a = motor.analisar(vinculo("2500.00", 44), jornadas, List.of(pago("2500.00")), INICIO, FIM, HOJE);
        Resultado b = motor.analisar(vinculo("2500.00", 44), jornadas, List.of(pago("2500.00")), INICIO, FIM, HOJE);

        assertThat(a).isEqualTo(b);
    }
}
