package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.IrregularidadeDetectada;
import br.com.fiap.trabalhojusto.model.RelatorioAnalise;
import br.com.fiap.trabalhojusto.model.StatusAnalise;
import br.com.fiap.trabalhojusto.model.TipoIrregularidade;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RelatorioDto {

    public static final String AVISO = "Este relatório aponta POSSÍVEIS irregularidades e valores de REFERÊNCIA "
            + "calculados a partir dos dados informados. Não é parecer jurídico nem garante direito a valores. "
            + "Procure o sindicato da categoria, a Defensoria Pública ou um advogado trabalhista.";

    private RelatorioDto() {
    }

    public record Request(
            @NotNull(message = "é obrigatório") LocalDate periodoInicio,
            @NotNull(message = "é obrigatório") LocalDate periodoFim) {
    }

    public record Resumo(Long id, Long vinculoId, LocalDate periodoInicio, LocalDate periodoFim, StatusAnalise status,
                         BigDecimal diferencaTotalEstimada, LocalDateTime solicitadoEm, LocalDateTime concluidoEm) {

        public static Resumo de(RelatorioAnalise r) {
            return new Resumo(r.getId(), r.getVinculo().getId(), r.getPeriodoInicio(), r.getPeriodoFim(), r.getStatus(),
                    r.getDiferencaTotalEstimada(), r.getSolicitadoEm(), r.getConcluidoEm());
        }
    }

    public record Irregularidade(TipoIrregularidade tipo, LocalDate dataReferencia, String descricao,
                                 BigDecimal valorEstimado) {

        public static Irregularidade de(IrregularidadeDetectada i) {
            return new Irregularidade(i.getTipo(), i.getDataReferencia(), i.getDescricao(), i.getValorEstimado());
        }
    }

    public record Detalhe(Long id, Long vinculoId, LocalDate periodoInicio, LocalDate periodoFim, StatusAnalise status,
                          String versaoRegras, BigDecimal diferencaTotalEstimada, String explicacaoSimples,
                          String mensagemErro, LocalDateTime solicitadoEm, LocalDateTime concluidoEm,
                          List<Irregularidade> irregularidades, String aviso) {

        public static Detalhe de(RelatorioAnalise r) {
            return new Detalhe(r.getId(), r.getVinculo().getId(), r.getPeriodoInicio(), r.getPeriodoFim(), r.getStatus(),
                    r.getVersaoRegras(), r.getDiferencaTotalEstimada(), r.getExplicacaoSimples(), r.getMensagemErro(),
                    r.getSolicitadoEm(), r.getConcluidoEm(),
                    r.getIrregularidades().stream().map(Irregularidade::de).toList(), AVISO);
        }
    }
}
