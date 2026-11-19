package br.com.fiap.trabalhojusto.security;

import br.com.fiap.trabalhojusto.model.Papel;
import br.com.fiap.trabalhojusto.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Gera e valida tokens JWT (HS256). A chave vem da variável de ambiente JWT_SECRET (mínimo 32 caracteres).
 * Se não for informada, uma chave aleatória é criada a cada execução (nada fica no código-fonte);
 * nesse caso os tokens deixam de valer quando a aplicação reinicia.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public record DadosToken(String email, Papel papel) {
    }

    private final SecretKey chave;
    private final long expiracaoSegundos;

    public JwtService(@Value("${jwt.secret:}") String segredo,
                      @Value("${jwt.expiration-seconds:3600}") long expiracaoSegundos) {
        this.expiracaoSegundos = expiracaoSegundos;
        if (segredo == null || segredo.isBlank()) {
            byte[] aleatorio = new byte[48];
            new SecureRandom().nextBytes(aleatorio);
            this.chave = Keys.hmacShaKeyFor(aleatorio);
            log.warn("JWT_SECRET não definido: usando chave aleatória desta execução. Defina JWT_SECRET para tokens persistentes.");
        } else {
            try {
                this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
            } catch (WeakKeyException e) {
                throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres", e);
            }
        }
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("papel", usuario.getPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
                .signWith(chave)
                .compact();
    }

    /** Vazio se o token for inválido, adulterado ou expirado. */
    public Optional<DadosToken> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new DadosToken(claims.getSubject(), Papel.valueOf(claims.get("papel", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
