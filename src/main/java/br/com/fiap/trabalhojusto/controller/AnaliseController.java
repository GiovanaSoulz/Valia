package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.dto.RelatorioDto;
import br.com.fiap.trabalhojusto.service.AnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Análises e relatórios")
public class AnaliseController {

    private final AnaliseService service;

    public AnaliseController(AnaliseService service) {
        this.service = service;
    }

    @Operation(summary = "Solicita a análise de um período",
            description = "Responde 202 com o relatório PENDENTE. O processamento é assíncrono (RabbitMQ); "
                    + "consulte GET /analises/{id} até o status ser CONCLUIDO.")
    @PostMapping("/vinculos/{vinculoId}/analises")
    public ResponseEntity<RelatorioDto.Resumo> solicitar(@PathVariable Long vinculoId,
                                                         @Valid @RequestBody RelatorioDto.Request req,
                                                         Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.solicitar(auth.getName(), vinculoId, req));
    }

    @Operation(summary = "Lista os relatórios de um vínculo (paginado)")
    @GetMapping("/vinculos/{vinculoId}/analises")
    public PaginaResponse<RelatorioDto.Resumo> listar(@PathVariable Long vinculoId,
                                                      @RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "10") int tamanho,
                                                      Authentication auth) {
        return service.listar(auth.getName(), vinculoId, pagina, tamanho);
    }

    @Operation(summary = "Detalha um relatório: irregularidades, valores de referência e explicação em linguagem simples")
    @GetMapping("/analises/{id}")
    public RelatorioDto.Detalhe buscar(@PathVariable Long id, Authentication auth) {
        return service.buscar(auth.getName(), id);
    }
}
