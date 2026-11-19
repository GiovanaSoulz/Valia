package br.com.fiap.trabalhojusto.regras;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conjunto de parâmetros legais vigentes a partir de uma data.
 * Todas as durações estão em minutos.
 */
public record VersaoRegras(
        String versao,
        LocalDate vigenciaInicio,
        BigDecimal salarioMinimo,
        BigDecimal adicionalHoraExtra,
        int jornadaDiariaMaximaMinutos,
        int limiteExtrasDiariasMinutos,
        int jornadaSemanalMaximaMinutos,
        int limiteJornadaParaIntervaloMinutos,
        int intervaloMinimoMinutos,
        int interjornadaMinimaMinutos) {

    /** Parâmetros que se repetem entre versões: 8h/dia, 44h/semana, +50%, 1h de intervalo, 11h entre jornadas. */
    public static VersaoRegras padrao(String versao, LocalDate vigenciaInicio, String salarioMinimo) {
        return new VersaoRegras(versao, vigenciaInicio, new BigDecimal(salarioMinimo),
                new BigDecimal("0.50"), 8 * 60, 2 * 60, 44 * 60, 6 * 60, 60, 11 * 60);
    }
}
