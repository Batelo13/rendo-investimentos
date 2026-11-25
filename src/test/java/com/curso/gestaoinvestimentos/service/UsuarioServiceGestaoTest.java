package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.UsuarioAdminRequestDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.security.AdministradoresConfigurados;
import com.curso.gestaoinvestimentos.security.ProtecaoCpf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Aula 14 (gestao de usuarios), escrito antes da implementacao (aula 15, TDD).
 * Mocks so nas dependencias: repositorios, encoder, HMAC do CPF, envio de
 * codigo e a lista ADMIN_EMAILS. Nunca a classe testada.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceGestaoTest {

    private static final String ADMIN = "chefe@example.com";

    @Mock private UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CarteiraRepository carteiraRepository;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private ProtecaoCpf protecaoCpf;
    @Mock private AdministradoresConfigurados administradores;
    @InjectMocks private UsuarioService service;

    private Usuario conta(Long id, String email, Role role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Conta " + id);
        u.setEmail(email);
        u.setRole(role);
        u.setAtivo(true);
        return u;
    }

    // ---------------------------- papel ----------------------------

    @Test
    void promoveUsuarioComumParaAdmin() {
        Usuario alvo = conta(2L, "ana@example.com", Role.USER);
        when(repository.findById(2L)).thenReturn(Optional.of(alvo));
        when(repository.save(alvo)).thenReturn(alvo);

        UsuarioResponseDTO r = service.alterarPapel(2L, Role.ADMIN, ADMIN);

        assertThat(r.role()).isEqualTo(Role.ADMIN);
        verify(repository).save(alvo);
    }

    @Test
    void recusaMudarOProprioPapel() {
        when(repository.findById(1L)).thenReturn(Optional.of(conta(1L, ADMIN, Role.ADMIN)));

        assertThatThrownBy(() -> service.alterarPapel(1L, Role.USER, ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("proprio papel");
        verify(repository, never()).save(any());
    }

    @Test
    void recusaRebaixarOUltimoAdmin() {
        when(repository.findById(3L)).thenReturn(Optional.of(conta(3L, "outro.admin@example.com", Role.ADMIN)));
        when(repository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarPapel(3L, Role.USER, ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ultimo administrador");
    }

    @Test
    void rebaixaAdminQuandoHaOutro() {
        Usuario alvo = conta(3L, "outro.admin@example.com", Role.ADMIN);
        when(repository.findById(3L)).thenReturn(Optional.of(alvo));
        when(repository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(repository.save(alvo)).thenReturn(alvo);

        assertThat(service.alterarPapel(3L, Role.USER, ADMIN).role()).isEqualTo(Role.USER);
    }

    @Test
    void recusaRebaixarAdminDoAdminEmails() {
        when(repository.findById(4L)).thenReturn(Optional.of(conta(4L, "dono@example.com", Role.ADMIN)));
        when(administradores.ehAdministrador("dono@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.alterarPapel(4L, Role.USER, ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ADMIN_EMAILS");
    }

    @Test
    void papelDeContaInexistenteVira404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarPapel(99L, Role.ADMIN, ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------------------------- senha ----------------------------

    @Test
    void redefinirSenhaGravaSoOHash() {
        Usuario alvo = conta(2L, "ana@example.com", Role.USER);
        alvo.setSenha("hash-antigo");
        when(repository.findById(2L)).thenReturn(Optional.of(alvo));
        when(passwordEncoder.encode("NovaSenha9")).thenReturn("hash-novo");

        service.redefinirSenha(2L, "NovaSenha9");

        assertThat(alvo.getSenha()).isEqualTo("hash-novo").isNotEqualTo("NovaSenha9");
        verify(repository).save(alvo);
    }

    // ---------------------------- bloqueio ----------------------------

    @Test
    void adminNaoBloqueiaASiMesmo() {
        when(repository.findById(1L)).thenReturn(Optional.of(conta(1L, ADMIN, Role.ADMIN)));

        assertThatThrownBy(() -> service.bloquear(1L, ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("propria conta");
        verify(repository, never()).save(any());
    }

    // ---------------------------- criacao ----------------------------

    private UsuarioAdminRequestDTO pedido(Role role) {
        return new UsuarioAdminRequestDTO("Nova Pessoa", "nova@example.com", "11144477735", "Senha123", role);
    }

    @Test
    void adminCriaContaVerificadaAtivaComCarteiraESemEnviarCodigo() {
        when(repository.findByEmail("nova@example.com")).thenReturn(Optional.empty());
        when(protecaoCpf.resumo("11144477735")).thenReturn("cpf-hmac");
        when(repository.findByCpf("cpf-hmac")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("hash");
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioResponseDTO r = service.criarPeloAdmin(pedido(Role.ADMIN));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(salvo.capture());
        assertThat(salvo.getValue().getEmailVerified()).isTrue();
        assertThat(salvo.getValue().getAtivo()).isTrue();
        assertThat(salvo.getValue().getCpf()).isEqualTo("cpf-hmac");
        assertThat(salvo.getValue().getSenha()).isEqualTo("hash");
        assertThat(r.role()).isEqualTo(Role.ADMIN);
        verify(carteiraRepository).save(any(Carteira.class));
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void adminNaoCriaContaComEmailRepetido() {
        when(repository.findByEmail("nova@example.com")).thenReturn(Optional.of(conta(5L, "nova@example.com", Role.USER)));

        assertThatThrownBy(() -> service.criarPeloAdmin(pedido(Role.USER)))
                .isInstanceOf(RecursoDuplicadoException.class);
        verify(repository, never()).saveAndFlush(any());
    }
}
