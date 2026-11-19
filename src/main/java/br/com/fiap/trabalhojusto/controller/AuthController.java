package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.AuthDto;
import br.com.fiap.trabalhojusto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
@SecurityRequirements
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um novo trabalhador")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.UsuarioResponse registrar(@Valid @RequestBody AuthDto.RegistroRequest req) {
        return service.registrar(req);
    }

    @Operation(summary = "Autentica e devolve o token JWT")
    @PostMapping("/login")
    public AuthDto.TokenResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return service.login(req);
    }
}
