package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.CorretoraRequestDTO;
import com.curso.gestaoinvestimentos.dto.CorretoraResponseDTO;
import com.curso.gestaoinvestimentos.service.CorretoraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/corretoras")
public class CorretoraController {

    private final CorretoraService service;

    public CorretoraController(CorretoraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CorretoraResponseDTO> criar(@Valid @RequestBody CorretoraRequestDTO dto) {
        CorretoraResponseDTO criada = service.criar(dto);
        URI location = URI.create("/corretoras/" + criada.id());
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public List<CorretoraResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CorretoraResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    public CorretoraResponseDTO buscarPorCnpj(@PathVariable String cnpj) {
        return service.buscarPorCnpj(cnpj);
    }
}
