package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.AcaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.HistoricoCotacaoResponseDTO;
import com.curso.gestaoinvestimentos.service.AcaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public Page<AcaoResponseDTO> listar(@PageableDefault(size = 20) Pageable pageable) {
        return service.listar(pageable);
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

    @GetMapping("/{id}/historico")
    public List<HistoricoCotacaoResponseDTO> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
