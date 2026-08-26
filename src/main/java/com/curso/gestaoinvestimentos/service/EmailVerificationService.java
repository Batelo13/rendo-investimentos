package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.model.EmailVerificationCode;
import com.curso.gestaoinvestimentos.model.TipoCodigo;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.EmailVerificationCodeRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final int EXPIRACAO_MINUTOS = 10;
    private static final int MAX_TENTATIVAS = 5;
    private static final int COOLDOWN_SEGUNDOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final EmailVerificationCodeRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(UsuarioRepository usuarioRepository,
                                     EmailVerificationCodeRepository codigoRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void gerarEEnviarCodigo(Usuario usuario) {
        String codigo = gerarCodigo();
        LocalDateTime agora = LocalDateTime.now();

        EmailVerificationCode entidade = new EmailVerificationCode();
        entidade.setUsuario(usuario);
        entidade.setTipo(TipoCodigo.VERIFICACAO_EMAIL);
        entidade.setCodigoHash(passwordEncoder.encode(codigo));
        entidade.setCriadoEm(agora);
        entidade.setExpiraEm(agora.plusMinutes(EXPIRACAO_MINUTOS));
        codigoRepository.save(entidade);

        emailService.enviarCodigoVerificacao(usuario.getEmail(), usuario.getNome(), codigo);
    }

    // noRollbackFor: incrementar tentativas (ou invalidar o codigo ao estourar
    // o limite) precisa ser persistido mesmo quando o metodo termina lancando
    // RegraDeNegocioException -- sem isso, o rollback padrao do @Transactional
    // desfaz o proprio incremento que a tentativa deveria registrar.
    @Transactional(noRollbackFor = RegraDeNegocioException.class)
    public void confirmarCodigo(String email, String codigoInformado) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com email " + email));

        if (Boolean.TRUE.equals(usuario.getEmailVerified())) {
            throw new RegraDeNegocioException("Esta conta ja esta verificada.");
        }

        EmailVerificationCode ativo = codigoRepository
                .findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL)
                .orElseThrow(() -> new RegraDeNegocioException("Codigo de verificacao invalido."));

        if (ativo.getTentativas() >= MAX_TENTATIVAS) {
            ativo.setUsado(true);
            codigoRepository.save(ativo);
            throw new RegraDeNegocioException("Numero maximo de tentativas excedido. Solicite um novo codigo.");
        }

        if (LocalDateTime.now().isAfter(ativo.getExpiraEm())) {
            throw new RegraDeNegocioException("O codigo expirou. Solicite um novo codigo.");
        }

        if (!passwordEncoder.matches(codigoInformado, ativo.getCodigoHash())) {
            ativo.setTentativas(ativo.getTentativas() + 1);
            codigoRepository.save(ativo);
            throw new RegraDeNegocioException("Codigo de verificacao invalido.");
        }

        ativo.setUsado(true);
        codigoRepository.save(ativo);
        usuario.setEmailVerified(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reenviarCodigo(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com email " + email));

        if (Boolean.TRUE.equals(usuario.getEmailVerified())) {
            throw new RegraDeNegocioException("Esta conta ja esta verificada.");
        }

        Optional<EmailVerificationCode> ultimo = codigoRepository
                .findFirstByUsuarioAndTipoOrderByCriadoEmDesc(usuario, TipoCodigo.VERIFICACAO_EMAIL);

        ultimo.ifPresent(codigo -> {
            long segundosDesdeUltimoEnvio = Duration.between(codigo.getCriadoEm(), LocalDateTime.now()).getSeconds();
            if (segundosDesdeUltimoEnvio < COOLDOWN_SEGUNDOS) {
                throw new RegraDeNegocioException("Aguarde antes de solicitar um novo codigo.");
            }
            if (!Boolean.TRUE.equals(codigo.getUsado())) {
                codigo.setUsado(true);
                codigoRepository.save(codigo);
            }
        });

        gerarEEnviarCodigo(usuario);
    }

    private String gerarCodigo() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
