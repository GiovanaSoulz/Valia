package br.com.fiap.trabalhojusto.controller;

import br.com.fiap.trabalhojusto.dto.IaDto;
import br.com.fiap.trabalhojusto.service.InterpretadorRelatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia")
@Tag(name = "IA (Spring AI)")
public class IaController {

    private final InterpretadorRelatoService service;

    public IaController(InterpretadorRelatoService service) {
        this.service = service;
    }

    @Operation(summary = "Interpreta um relato em texto livre e sugere os dados do vínculo e da jornada",
            description = "Exemplo: \"Trabalho como diarista de segunda a sexta, entro às 7h e saio às 18h com meia hora de almoço, ganho 1800 por mês, sem carteira assinada.\"")
    @PostMapping("/interpretar-relato")
    public IaDto.InterpretacaoResponse interpretar(@Valid @RequestBody IaDto.InterpretarRequest req) {
        return service.interpretar(req.texto());
    }
}
