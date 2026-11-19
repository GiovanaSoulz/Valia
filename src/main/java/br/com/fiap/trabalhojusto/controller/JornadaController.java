package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.JornadaDto;
import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.service.JornadaService;
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
@RequestMapping("/vinculos/{vinculoId}/jornadas")
@Tag(name = "Jornadas")
public class JornadaController {

    private final JornadaService service;

    public JornadaController(JornadaService service) {
        this.service = service;
    }

    @Operation(summary = "Registra a jornada de um dia (uma por data)")
    @PostMapping
    public ResponseEntity<JornadaDto.Response> criar(@PathVariable Long vinculoId,
                                                     @Valid @RequestBody JornadaDto.Request req, Authentication auth) {
        JornadaDto.Response criada = service.criar(auth.getName(), vinculoId, req);
        return ResponseEntity.created(URI.create("/vinculos/" + vinculoId + "/jornadas/" + criada.id())).body(criada);
    }

    @GetMapping
    public PaginaResponse<JornadaDto.Response> listar(@PathVariable Long vinculoId,
                                                      @RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "10") int tamanho,
                                                      Authentication auth) {
        return service.listar(auth.getName(), vinculoId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public JornadaDto.Response buscar(@PathVariable Long vinculoId, @PathVariable Long id, Authentication auth) {
        return service.buscar(auth.getName(), vinculoId, id);
    }

    @PutMapping("/{id}")
    public JornadaDto.Response atualizar(@PathVariable Long vinculoId, @PathVariable Long id,
                                         @Valid @RequestBody JornadaDto.Request req, Authentication auth) {
        return service.atualizar(auth.getName(), vinculoId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long vinculoId, @PathVariable Long id, Authentication auth) {
        service.excluir(auth.getName(), vinculoId, id);
        return ResponseEntity.noContent().build();
    }
}
