package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.AlterarPapelRequestDTO;
import com.curso.gestaoinvestimentos.dto.CadastroResponseDTO;
import com.curso.gestaoinvestimentos.dto.RedefinirSenhaRequestDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioAdminRequestDTO;
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
import com.curso.gestaoinvestimentos.dto.EstatisticasUsuariosDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    // Lista todas as contas (nome, email, papel, status): dado de terceiros, so ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UsuarioResponseDTO> listar(@PageableDefault(size = 20) Pageable pageable) {
        return service.listar(pageable);
    }

    // Dono do registro ou ADMIN (Modulo 11: controle de acesso por propriedade,
    // nao so por papel). AccessDeniedException vira 403 pelo Spring Security.
    // Painel do administrador: so contagens, nenhum dado pessoal.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estatisticas")
    public EstatisticasUsuariosDTO estatisticas() {
        return service.estatisticas();
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long id, Authentication autenticacao) {
        UsuarioResponseDTO usuario = service.buscarPorId(id);
        boolean admin = autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !usuario.email().equalsIgnoreCase(autenticacao.getName())) {
            throw new AccessDeniedException("Acesso negado ao registro de outro usuario");
        }
        return usuario;
    }

    // Bloquear/desbloquear = desativar sem apagar (aula 14): so ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/bloquear")
    public UsuarioResponseDTO bloquear(@PathVariable Long id, Authentication autenticacao) {
        return service.bloquear(id, autenticacao.getName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/desbloquear")
    public UsuarioResponseDTO desbloquear(@PathVariable Long id) {
        return service.desbloquear(id);
    }

    // Aula 14 (gestao de usuarios): criar conta com papel escolhido -- dar
    // acesso e papel e decisao de administrador, so ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO criarPeloAdmin(@Valid @RequestBody UsuarioAdminRequestDTO dto) {
        return service.criarPeloAdmin(dto);
    }

    // Promover/rebaixar muda o que a conta pode fazer: so ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/papel")
    public UsuarioResponseDTO alterarPapel(@PathVariable Long id, @Valid @RequestBody AlterarPapelRequestDTO dto,
                                           Authentication autenticacao) {
        return service.alterarPapel(id, dto.role(), autenticacao.getName());
    }

    // Redefinir senha de outra conta: so ADMIN; 204 sem corpo (nada de senha/hash na resposta).
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        service.redefinirSenha(id, dto.senha());
    }
}
