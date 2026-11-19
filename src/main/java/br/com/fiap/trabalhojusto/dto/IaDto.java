package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public final class IaDto {

    private IaDto() {
    }

    public record InterpretarRequest(
            @NotBlank(message = "é obrigatório") @Size(max = 2000, message = "deve ter no máximo 2000 caracteres") String texto) {
    }

    /** Sugestão para pré-preencher vínculo e jornada no app. Sempre deve ser confirmada pela pessoa. */
    public record InterpretacaoResponse(TipoContratacao tipoContratacao, BigDecimal salarioMensal,
                                        Integer jornadaSemanalHoras, Integer diasPorSemana, LocalTime horaEntrada,
                                        LocalTime horaSaida, Integer intervaloMinutos, String observacoes,
                                        List<String> camposNaoIdentificados, String aviso) {
    }
}
