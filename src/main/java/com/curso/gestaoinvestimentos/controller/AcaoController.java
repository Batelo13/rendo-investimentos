package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.AcaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.service.AcaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/acoes")
public class AcaoController {

    private final AcaoService service;

    public AcaoController(AcaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AcaoResponseDTO> criar(@Valid @RequestBody AcaoRequestDTO dto) {
        AcaoResponseDTO criada = service.criar(dto);
        URI location = URI.create("/acoes/" + criada.id());
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public List<AcaoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AcaoResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/ticker/{ticker}")
    public AcaoResponseDTO buscarPorTicker(@PathVariable String ticker) {
        return service.buscarPorTicker(ticker);
    }

    @PutMapping("/{id}/atualizar-cotacao")
    public AcaoResponseDTO atualizarCotacao(@PathVariable Long id) {
        return service.atualizarCotacao(id);
    }
}
