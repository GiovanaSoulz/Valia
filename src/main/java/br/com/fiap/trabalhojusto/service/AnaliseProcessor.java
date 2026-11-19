package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.model.IrregularidadeDetectada;
import br.com.fiap.trabalhojusto.model.Pagamento;
import br.com.fiap.trabalhojusto.model.RegistroJornada;
import br.com.fiap.trabalhojusto.model.RelatorioAnalise;
import br.com.fiap.trabalhojusto.model.StatusAnalise;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.regras.MotorRegras;
import br.com.fiap.trabalhojusto.repository.JornadaRepository;
import br.com.fiap.trabalhojusto.repository.PagamentoRepository;
import br.com.fiap.trabalhojusto.repository.RelatorioAnaliseRepository;
import br.com.fiap.trabalhojusto.util.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Executa a análise de um relatório PENDENTE: motor de regras (determinístico) + explicação em linguagem simples. */
@Service
public class AnaliseProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnaliseProcessor.class);

    private final RelatorioAnaliseRepository relatorios;
    private final JornadaRepository jornadas;
    private final PagamentoRepository pagamentos;
    private final MotorRegras motor;
    private final ExplicacaoIaService explicacaoIa;

    public AnaliseProcessor(RelatorioAnaliseRepository relatorios, JornadaRepository jornadas,
                            PagamentoRepository pagamentos, MotorRegras motor, ExplicacaoIaService explicacaoIa) {
        this.relatorios = relatorios;
        this.jornadas = jornadas;
        this.pagamentos = pagamentos;
        this.motor = motor;
        this.explicacaoIa = explicacaoIa;
    }

    @Transactional
    public void processar(Long relatorioId) {
        RelatorioAnalise relatorio = relatorios.findWithVinculoById(relatorioId).orElse(null);
        if (relatorio == null) {
            log.warn("Relatório {} não encontrado; evento ignorado", relatorioId);
            return;
        }
        if (relatorio.getStatus() != StatusAnalise.PENDENTE) {
            return; // idempotência: evento duplicado não reprocessa
        }
        try {
            VinculoTrabalhista v = relatorio.getVinculo();
            List<RegistroJornada> registros = jornadas.findByVinculoIdAndDataBetweenOrderByDataAsc(
                    v.getId(), relatorio.getPeriodoInicio(), relatorio.getPeriodoFim());
            List<Pagamento> pagos = pagamentos.findByVinculoIdAndCompetenciaBetween(
                    v.getId(), relatorio.getPeriodoInicio().withDayOfMonth(1), relatorio.getPeriodoFim());

            MotorRegras.Resultado resultado = motor.analisar(
                    new MotorRegras.DadosVinculo(v.getTipoContratacao(), v.getSalarioMensal(),
                            v.getJornadaSemanalHoras(), v.getDataInicio()),
                    registros.stream()
                            .map(j -> new MotorRegras.Jornada(j.getData(), j.getHoraEntrada(), j.getHoraSaida(), j.getIntervaloMinutos()))
                            .toList(),
                    pagos.stream()
                            .map(p -> new MotorRegras.PagamentoDado(p.getCompetencia(), p.getValorPago()))
                            .toList(),
                    relatorio.getPeriodoInicio(), relatorio.getPeriodoFim(), Tempo.hoje());

            relatorio.setVersaoRegras(resultado.versaoRegras());
            relatorio.setDiferencaTotalEstimada(resultado.diferencaTotalEstimada());
            for (MotorRegras.Achado a : resultado.achados()) {
                IrregularidadeDetectada i = new IrregularidadeDetectada();
                i.setTipo(a.tipo());
                i.setDataReferencia(a.referencia());
                i.setDescricao(a.descricao());
                i.setValorEstimado(a.valorEstimado());
                relatorio.adicionar(i);
            }
            relatorio.setExplicacaoSimples(explicacaoIa.explicar(v.getTipoContratacao(), resultado));
            relatorio.setStatus(StatusAnalise.CONCLUIDO);
            relatorio.setConcluidoEm(LocalDateTime.now());
        } catch (Exception e) {
            // não relança: evita a mensagem voltar para a fila em loop
            log.error("Falha ao processar relatório {}", relatorioId, e);
            relatorio.getIrregularidades().clear();
            relatorio.setStatus(StatusAnalise.ERRO);
            relatorio.setMensagemErro("Não foi possível concluir a análise. Tente solicitar novamente.");
        }
        relatorios.save(relatorio);
    }
}
