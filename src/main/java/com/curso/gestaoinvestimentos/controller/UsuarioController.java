package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.CadastroResponseDTO;
import com.curso.gestaoinvestimentos.dto.MensagemResponseDTO;
import com.curso.gestaoinvestimentos.dto.ReenviarCodigoRequestDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioResponseDTO;
import com.curso.gestaoinvestimentos.dto.VerificarEmailRequestDTO;
import com.curso.gestaoinvestimentos.service.EmailVerificationService;
import com.curso.gestaoinvestimentos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;
    private final EmailVerificationService emailVerificationService;

    public UsuarioController(UsuarioService service, EmailVerificationService emailVerificationService) {
        this.service = service;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CadastroResponseDTO cadastrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        return service.cadastrar(dto);
    }

    @PostMapping("/verificar-email")
    public MensagemResponseDTO verificarEmail(@Valid @RequestBody VerificarEmailRequestDTO dto) {
        emailVerificationService.confirmarCodigo(dto.email(), dto.codigo());
        return new MensagemResponseDTO("E-mail verificado com sucesso.");
    }

    @PostMapping("/reenviar-codigo")
    public MensagemResponseDTO reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequestDTO dto) {
        emailVerificationService.reenviarCodigo(dto.email());
        return new MensagemResponseDTO("Enviamos um novo codigo de verificacao para seu e-mail.");
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
