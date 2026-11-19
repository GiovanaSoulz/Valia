package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.GestorDto;
import br.com.fiap.trabalhojusto.service.GestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gestor")
@Tag(name = "Painel do gestor")
public class GestorController {

    private final GestorService service;

    public GestorController(GestorService service) {
        this.service = service;
    }

    @Operation(summary = "Estatísticas agregadas e anonimizadas (somente papel GESTOR)")
    @GetMapping("/estatisticas")
    public GestorDto.Estatisticas estatisticas() {
        return service.estatisticas();
    }
}
