package br.com.fiap.trabalhojusto.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginacao {

    private static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    public static Pageable de(int pagina, int tamanho, Sort ordenacao) {
        int p = Math.max(pagina, 0);
        int t = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        return PageRequest.of(p, t, ordenacao);
    }
}
