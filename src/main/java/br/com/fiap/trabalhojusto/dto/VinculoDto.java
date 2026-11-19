package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class VinculoDto {

    private VinculoDto() {
    }

    public record Request(
            @NotBlank(message = "é obrigatório") @Size(max = 120, message = "deve ter no máximo 120 caracteres") String empregador,
            @NotNull(message = "é obrigatório") TipoContratacao tipoContratacao,
            @NotNull(message = "é obrigatório") @Positive(message = "deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "valor inválido") BigDecimal salarioMensal,
            @NotNull(message = "é obrigatório") @Min(value = 1, message = "mínimo de 1 hora") @Max(value = 44, message = "máximo de 44 horas semanais") Integer jornadaSemanalHoras,
            @NotNull(message = "é obrigatório") @PastOrPresent(message = "não pode ser uma data futura") LocalDate dataInicio,
            Boolean ativo) {
    }

    public record Response(Long id, String empregador, TipoContratacao tipoContratacao, BigDecimal salarioMensal,
                           Integer jornadaSemanalHoras, LocalDate dataInicio, boolean ativo) {

        public static Response de(VinculoTrabalhista v) {
            return new Response(v.getId(), v.getEmpregador(), v.getTipoContratacao(), v.getSalarioMensal(),
                    v.getJornadaSemanalHoras(), v.getDataInicio(), v.isAtivo());
        }
    }
}
