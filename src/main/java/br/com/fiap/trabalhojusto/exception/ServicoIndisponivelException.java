package br.com.fiap.trabalhojusto.exception;

/** Dependência externa (IA, fila) indisponível: vira HTTP 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
