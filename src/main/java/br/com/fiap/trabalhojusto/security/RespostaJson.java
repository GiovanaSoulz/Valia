package br.com.fiap.trabalhojusto.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

/** Escreve erros de segurança (401/403) no mesmo formato do ErroResponse, sem depender de serializador JSON. */
final class RespostaJson {

    private RespostaJson() {
    }

    static void erro(HttpServletResponse response, int status, String erro, String mensagem, String caminho)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"erro\":\"" + escapar(erro) + "\",\"mensagem\":\"" + escapar(mensagem)
                + "\",\"caminho\":\"" + escapar(caminho) + "\",\"detalhes\":[]}";
        response.getWriter().write(json);
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
