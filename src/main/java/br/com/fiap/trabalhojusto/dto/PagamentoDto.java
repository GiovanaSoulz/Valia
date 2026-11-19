package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.Pagamento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PagamentoDto {

    private PagamentoDto() {
    }

    /** competencia: qualquer dia do mês de referência (é normalizada para o dia 1). */
    public record Request(
            @NotNull(message = "é obrigatório") LocalDate competencia,
            @NotNull(message = "é obrigatório") @PositiveOrZero(message = "não pode ser negativo")
            @Digits(integer = 10, fraction = 2, message = "valor inválido") BigDecimal valorPago,
            @PastOrPresent(message = "não pode ser uma data futura") LocalDate dataPagamento) {
    }

    public record Response(Long id, Long vinculoId, LocalDate competencia, BigDecimal valorPago,
                           LocalDate dataPagamento) {

        public static Response de(Pagamento p) {
            return new Response(p.getId(), p.getVinculo().getId(), p.getCompetencia(), p.getValorPago(),
                    p.getDataPagamento());
        }
    }
}
