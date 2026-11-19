package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.dto.VinculoDto;
import br.com.fiap.trabalhojusto.service.VinculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/vinculos")
@Tag(name = "Vínculos trabalhistas")
public class VinculoController {

    private final VinculoService service;

    public VinculoController(VinculoService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um vínculo de trabalho")
    @PostMapping
    public ResponseEntity<VinculoDto.Response> criar(@Valid @RequestBody VinculoDto.Request req, Authentication auth) {
        VinculoDto.Response criado = service.criar(auth.getName(), req);
        return ResponseEntity.created(URI.create("/vinculos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista os vínculos do trabalhador autenticado (paginado)")
    @GetMapping
    public PaginaResponse<VinculoDto.Response> listar(@RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "10") int tamanho,
                                                      Authentication auth) {
        return service.listar(auth.getName(), pagina, tamanho);
    }

    @GetMapping("/{id}")
    public VinculoDto.Response buscar(@PathVariable Long id, Authentication auth) {
        return service.buscar(auth.getName(), id);
    }

    @PutMapping("/{id}")
    public VinculoDto.Response atualizar(@PathVariable Long id, @Valid @RequestBody VinculoDto.Request req,
                                         Authentication auth) {
        return service.atualizar(auth.getName(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication auth) {
        service.excluir(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
