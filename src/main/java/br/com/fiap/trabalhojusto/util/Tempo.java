package br.com.fiap.trabalhojusto.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class Tempo {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private Tempo() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(ZONA);
    }
}
