package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioResponseDTO;
import com.curso.gestaoinvestimentos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO criado = service.cadastrar(dto);
        URI location = URI.create("/usuarios/" + criado.id());
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    public Page<UsuarioResponseDTO> listar(@PageableDefault(size = 20) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/bloquear")
    public UsuarioResponseDTO bloquear(@PathVariable Long id) {
        return service.bloquear(id);
    }

    @PatchMapping("/{id}/desbloquear")
    public UsuarioResponseDTO desbloquear(@PathVariable Long id) {
        return service.desbloquear(id);
    }
}
