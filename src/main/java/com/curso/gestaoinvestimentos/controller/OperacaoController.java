package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.service.OperacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/operacoes")
public class OperacaoController {

    private final OperacaoService service;

    public OperacaoController(OperacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OperacaoResponseDTO> registrar(Principal principal, @Valid @RequestBody OperacaoRequestDTO dto) {
        OperacaoResponseDTO criada = service.registrar(principal.getName(), dto);
        URI location = URI.create("/operacoes/" + criada.id());
        return ResponseEntity.created(location).body(criada);
    }

    @PatchMapping("/{id}/cancelar")
    public OperacaoResponseDTO cancelar(Principal principal, @PathVariable Long id) {
        return service.cancelar(id, principal.getName());
    }
}
