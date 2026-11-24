package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CadastroResponseDTO;
import com.curso.gestaoinvestimentos.dto.EstatisticasUsuariosDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    // Saldo virtual fictício com que toda carteira nova começa -- simulação
    // academica, nenhum dinheiro real envolvido.
    private static final BigDecimal SALDO_INICIAL_PADRAO = new BigDecimal("100000.00");

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CarteiraRepository carteiraRepository;
    private final EmailVerificationService emailVerificationService;
    private final ProtecaoCpf protecaoCpf;
    private final AdministradoresConfigurados administradores;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                           CarteiraRepository carteiraRepository, EmailVerificationService emailVerificationService,
                           ProtecaoCpf protecaoCpf, AdministradoresConfigurados administradores) {
        this.administradores = administradores;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.carteiraRepository = carteiraRepository;
        this.emailVerificationService = emailVerificationService;
        this.protecaoCpf = protecaoCpf;
    }

    @Transactional
    public CadastroResponseDTO cadastrar(UsuarioRequestDTO dto) {
        repository.findByEmail(dto.email()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email " + dto.email());
        });
        String cpfProtegido = protecaoCpf.resumo(dto.cpf());
        repository.findByCpf(cpfProtegido).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com este CPF");
        });

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCpf(cpfProtegido);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        // Campos controlados pelo sistema, nunca pelo cliente:
        usuario.setRole(administradores.ehAdministrador(dto.email()) ? Role.ADMIN : Role.USER);
        usuario.setAtivo(true);
        usuario.setEmailVerified(false);
        usuario.setDataCadastro(LocalDate.now());

        // A checagem acima nao cobre dois cadastros iguais ao mesmo tempo (ex.:
        // clique duplo): os dois passam por ela e quem barra e a restricao unique
        // do banco. Essa colisao e conflito (409), nao erro interno (500).
        Usuario salvo;
        try {
            salvo = repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException concorrente) {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com este email ou CPF");
        }

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(SALDO_INICIAL_PADRAO);
        carteiraRepository.save(carteira);

        emailVerificationService.gerarEEnviarCodigo(salvo);

        return new CadastroResponseDTO(
                "Conta criada. Enviamos um codigo de verificacao para seu e-mail.", true);
    }

    public Page<UsuarioResponseDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponseDTO);
    }

    public EstatisticasUsuariosDTO estatisticas() {
        return new EstatisticasUsuariosDTO(
                repository.count(),
                repository.countByAtivo(true),
                repository.countByAtivo(false),
                repository.countByEmailVerified(false),
                repository.countByRole(Role.ADMIN),
                repository.countByDataCadastroGreaterThanEqual(LocalDate.now().minusDays(30)));
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com id " + id));
        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO bloquear(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com id " + id));
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Usuario " + id + " ja esta bloqueado");
        }
        usuario.setAtivo(false);
        return toResponseDTO(repository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO desbloquear(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com id " + id));
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Usuario " + id + " ja esta ativo");
        }
        usuario.setAtivo(true);
        return toResponseDTO(repository.save(usuario));
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getDataCadastro()
        );
    }
}
