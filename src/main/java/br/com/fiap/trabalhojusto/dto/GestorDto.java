package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.TipoIrregularidade;

import java.math.BigDecimal;
import java.util.List;

/** Somente dados agregados e anonimizados: o gestor nunca vê dados individuais de trabalhadores. */
public final class GestorDto {

    private GestorDto() {
    }

    public record PorContratacao(TipoContratacao tipoContratacao, long quantidade) {
    }

    public record PorIrregularidade(TipoIrregularidade tipo, long quantidade, BigDecimal valorEstimadoTotal) {
    }

    public record Estatisticas(long totalTrabalhadores, long totalVinculos, long totalRelatoriosConcluidos,
                               BigDecimal diferencaTotalEstimada, List<PorContratacao> vinculosPorContratacao,
                               List<PorIrregularidade> irregularidadesPorTipo) {
    }
}
