package br.com.fiap.trabalhojusto.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de erro para toda a API. */
public record ErroResponse(LocalDateTime timestamp, int status, String erro, String mensagem, String caminho,
                           List<String> detalhes) {
}
