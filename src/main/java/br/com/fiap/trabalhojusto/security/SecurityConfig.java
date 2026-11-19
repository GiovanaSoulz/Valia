package br.com.fiap.trabalhojusto.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso:
 * - /auth/**, Swagger e /error: públicos
 * - /gestor/**: somente papel GESTOR (dados agregados e anonimizados)
 * - demais endpoints: somente papel TRABALHADOR, sempre restritos aos próprios dados
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** O login é feito pelo AuthService; este bean evita a senha gerada automaticamente pelo Spring Boot. */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticação feita via JWT");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/gestor/**").hasRole("GESTOR")
                        .anyRequest().hasRole("TRABALHADOR"))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, ex) -> RespostaJson.erro(res, 401, "Unauthorized",
                                "Autenticação necessária ou token inválido/expirado", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> RespostaJson.erro(res, 403, "Forbidden",
                                "Acesso negado para o seu perfil", req.getRequestURI())))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
