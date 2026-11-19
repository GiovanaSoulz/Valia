package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.AuthDto;
import br.com.fiap.trabalhojusto.exception.RegraNegocioException;
import br.com.fiap.trabalhojusto.model.Papel;
import br.com.fiap.trabalhojusto.model.Usuario;
import br.com.fiap.trabalhojusto.repository.UsuarioRepository;
import br.com.fiap.trabalhojusto.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /** Cadastro público: sempre cria TRABALHADOR. Contas de GESTOR não são criadas por este endpoint. */
    public AuthDto.UsuarioResponse registrar(AuthDto.RegistroRequest req) {
        String email = normalizar(req.email());
        if (usuarios.existsByEmail(email)) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPapel(Papel.TRABALHADOR);
        Usuario salvo = usuarios.save(u);
        return new AuthDto.UsuarioResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getPapel());
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        Usuario u = usuarios.findByEmail(normalizar(req.email()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!encoder.matches(req.senha(), u.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return new AuthDto.TokenResponse(jwtService.gerar(u), "Bearer", jwtService.getExpiracaoSegundos(), u.getPapel());
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
