package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CadastroResponseDTO;
import com.curso.gestaoinvestimentos.dto.EstatisticasUsuariosDTO;
import com.curso.gestaoinvestimentos.dto.UsuarioAdminRequestDTO;
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
        // Cadastro publico: papel e verificacao controlados pelo sistema, nunca pelo cliente.
        Usuario salvo = novaConta(dto.nome(), dto.email(), dto.cpf(), dto.senha(), Role.USER, false);
        emailVerificationService.gerarEEnviarCodigo(salvo);

        return new CadastroResponseDTO(
                "Conta criada. Enviamos um codigo de verificacao para seu e-mail.", true);
    }

    /** Aula 14: conta criada por um ADMIN ja nasce verificada (o admin responde por ela). */
    @Transactional
    public UsuarioResponseDTO criarPeloAdmin(UsuarioAdminRequestDTO dto) {
        return toResponseDTO(novaConta(dto.nome(), dto.email(), dto.cpf(), dto.senha(), dto.role(), true));
    }

    private Usuario novaConta(String nome, String email, String cpf, String senha, Role papel, boolean verificado) {
        repository.findByEmail(email).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email " + email);
        });
        String cpfProtegido = protecaoCpf.resumo(cpf);
        repository.findByCpf(cpfProtegido).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com este CPF");
        });

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setCpf(cpfProtegido);
        usuario.setSenha(passwordEncoder.encode(senha));
        // ADMIN_EMAILS sempre vence: email configurado e ADMIN em qualquer caminho.
        usuario.setRole(administradores.ehAdministrador(email) ? Role.ADMIN : papel);
        usuario.setAtivo(true);
        usuario.setEmailVerified(verificado);
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
        return salvo;
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

    /**
     * Aula 14: promover/rebaixar. Nunca o proprio papel, nunca sem nenhum ADMIN,
     * e nunca rebaixar email de ADMIN_EMAILS (voltaria a ADMIN ao reiniciar).
     */
    @Transactional
    public UsuarioResponseDTO alterarPapel(Long id, Role novoPapel, String emailAdmin) {
        Usuario usuario = buscarEntidade(id);
        if (usuario.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new RegraDeNegocioException("O administrador nao pode mudar o proprio papel");
        }
        if (usuario.getRole() == Role.ADMIN && novoPapel != Role.ADMIN) {
            if (administradores.ehAdministrador(usuario.getEmail())) {
                throw new RegraDeNegocioException("Este email esta em ADMIN_EMAILS: o papel ADMIN vem da configuracao do servidor");
            }
            if (repository.countByRole(Role.ADMIN) <= 1) {
                throw new RegraDeNegocioException("Nao e possivel rebaixar o ultimo administrador");
            }
        }
        usuario.setRole(novoPapel);
        return toResponseDTO(repository.save(usuario));
    }

    /** Aula 14: so o hash vai para o banco; a resposta HTTP e 204, sem corpo. */
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = buscarEntidade(id);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        repository.save(usuario);
    }

    @Transactional
    public UsuarioResponseDTO bloquear(Long id, String emailAdmin) {
        Usuario usuario = buscarEntidade(id);
        if (usuario.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new RegraDeNegocioException("O administrador nao pode bloquear a propria conta");
        }
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

    private Usuario buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado com id " + id));
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
