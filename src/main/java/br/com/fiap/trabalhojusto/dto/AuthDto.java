package br.com.fiap.trabalhojusto.dto;

import br.com.fiap.trabalhojusto.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {
    }

    public record RegistroRequest(
            @NotBlank(message = "é obrigatório") @Size(max = 120, message = "deve ter no máximo 120 caracteres") String nome,
            @NotBlank(message = "é obrigatório") @Email(message = "e-mail inválido") @Size(max = 160, message = "deve ter no máximo 160 caracteres") String email,
            @NotBlank(message = "é obrigatório") @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String senha) {
    }

    public record LoginRequest(
            @NotBlank(message = "é obrigatório") @Email(message = "e-mail inválido") String email,
            @NotBlank(message = "é obrigatório") String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Papel papel) {
    }

    public record TokenResponse(String token, String tipo, long expiraEmSegundos, Papel papel) {
    }
}
