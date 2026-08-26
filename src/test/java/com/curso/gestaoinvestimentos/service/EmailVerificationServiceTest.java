package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.model.EmailVerificationCode;
import com.curso.gestaoinvestimentos.model.TipoCodigo;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.EmailVerificationCodeRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailVerificationCodeRepository codigoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(usuarioRepository, codigoRepository, passwordEncoder, emailService);
    }

    private Usuario usuarioNaoVerificado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setEmailVerified(false);
        return usuario;
    }

    private EmailVerificationCode codigoAtivo(String hash, LocalDateTime expiraEm, int tentativas) {
        EmailVerificationCode codigo = new EmailVerificationCode();
        codigo.setId(1L);
        codigo.setTipo(TipoCodigo.VERIFICACAO_EMAIL);
        codigo.setCodigoHash(hash);
        codigo.setCriadoEm(LocalDateTime.now().minusMinutes(1));
        codigo.setExpiraEm(expiraEm);
        codigo.setUsado(false);
        codigo.setTentativas(tentativas);
        return codigo;
    }

    @Test
    void codigoCorretoVerificaAConta() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode codigo = codigoAtivo("hash", LocalDateTime.now().plusMinutes(5), 0);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(codigo));
        when(passwordEncoder.matches("483921", "hash")).thenReturn(true);

        service.confirmarCodigo("teste@example.com", "483921");

        assertTrue(codigo.getUsado());
        assertEquals(Boolean.TRUE, usuario.getEmailVerified());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void codigoErradoNaoVerificaEIncrementaTentativa() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode codigo = codigoAtivo("hash", LocalDateTime.now().plusMinutes(5), 0);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(codigo));
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);

        assertThrows(RegraDeNegocioException.class, () -> service.confirmarCodigo("teste@example.com", "000000"));

        assertEquals(1, codigo.getTentativas());
        assertFalse(codigo.getUsado());
        assertEquals(Boolean.FALSE, usuario.getEmailVerified());
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void codigoExpiradoERejeitado() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode codigo = codigoAtivo("hash", LocalDateTime.now().minusMinutes(1), 0);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(codigo));

        assertThrows(RegraDeNegocioException.class, () -> service.confirmarCodigo("teste@example.com", "483921"));

        assertEquals(Boolean.FALSE, usuario.getEmailVerified());
    }

    @Test
    void codigoJaUsadoNaoPodeSerReutilizado() {
        Usuario usuario = usuarioNaoVerificado();

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class, () -> service.confirmarCodigo("teste@example.com", "483921"));
    }

    @Test
    void contaJaVerificadaNaoPrecisaConfirmarNovamente() {
        Usuario usuario = usuarioNaoVerificado();
        usuario.setEmailVerified(true);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(RegraDeNegocioException.class, () -> service.confirmarCodigo("teste@example.com", "483921"));
        verify(codigoRepository, never()).findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(any(), any());
    }

    @Test
    void limiteDeTentativasEExcedido() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode codigo = codigoAtivo("hash", LocalDateTime.now().plusMinutes(5), 5);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(codigo));

        assertThrows(RegraDeNegocioException.class, () -> service.confirmarCodigo("teste@example.com", "483921"));

        assertTrue(codigo.getUsado());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void usuarioNaoEncontradoNaConfirmacao() {
        when(usuarioRepository.findByEmail("inexistente@example.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.confirmarCodigo("inexistente@example.com", "483921"));
    }

    @Test
    void reenvioGeraNovoCodigoEInvalidaOAnterior() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode anterior = codigoAtivo("hash-antigo", LocalDateTime.now().plusMinutes(5), 0);
        anterior.setCriadoEm(LocalDateTime.now().minusSeconds(120));

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(anterior));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-novo");

        service.reenviarCodigo("teste@example.com");

        assertTrue(anterior.getUsado());
        ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codigoRepository, times(2)).save(captor.capture());
        verify(emailService).enviarCodigoVerificacao(eq("teste@example.com"), anyString(), anyString());
    }

    @Test
    void cooldownDeReenvioEhRespeitado() {
        Usuario usuario = usuarioNaoVerificado();
        EmailVerificationCode recente = codigoAtivo("hash", LocalDateTime.now().plusMinutes(5), 0);
        recente.setCriadoEm(LocalDateTime.now().minusSeconds(10));

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioAndTipoOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL))
                .thenReturn(Optional.of(recente));

        assertThrows(RegraDeNegocioException.class, () -> service.reenviarCodigo("teste@example.com"));

        verify(emailService, never()).enviarCodigoVerificacao(anyString(), anyString(), anyString());
    }

    @Test
    void reenvioParaContaJaVerificadaERejeitado() {
        Usuario usuario = usuarioNaoVerificado();
        usuario.setEmailVerified(true);

        when(usuarioRepository.findByEmail("teste@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(RegraDeNegocioException.class, () -> service.reenviarCodigo("teste@example.com"));
        verify(codigoRepository, never()).findFirstByUsuarioAndTipoOrderByCriadoEmDesc(any(), any());
    }
}
