package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.RegistroJornada;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.LocalTime;

public final class JornadaDto {

    private JornadaDto() {
    }

    public record Request(
            @NotNull(message = "é obrigatório") @PastOrPresent(message = "não pode ser uma data futura") LocalDate data,
            @NotNull(message = "é obrigatório") LocalTime horaEntrada,
            @NotNull(message = "é obrigatório") LocalTime horaSaida,
            @NotNull(message = "é obrigatório") @Min(value = 0, message = "não pode ser negativo") @Max(value = 240, message = "máximo de 240 minutos") Integer intervaloMinutos) {
    }

    public record Response(Long id, Long vinculoId, LocalDate data, LocalTime horaEntrada, LocalTime horaSaida,
                           Integer intervaloMinutos) {

        public static Response de(RegistroJornada j) {
            return new Response(j.getId(), j.getVinculo().getId(), j.getData(), j.getHoraEntrada(), j.getHoraSaida(),
                    j.getIntervaloMinutos());
        }
    }
}
