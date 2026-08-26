package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CadastroResponseDTO;
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

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                           CarteiraRepository carteiraRepository, EmailVerificationService emailVerificationService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.carteiraRepository = carteiraRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public CadastroResponseDTO cadastrar(UsuarioRequestDTO dto) {
        repository.findByEmail(dto.email()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email " + dto.email());
        });
        repository.findByCpf(dto.cpf()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o CPF " + dto.cpf());
        });

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCpf(dto.cpf());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        // Campos controlados pelo sistema, nunca pelo cliente:
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        usuario.setEmailVerified(false);
        usuario.setDataCadastro(LocalDate.now());

        Usuario salvo = repository.save(usuario);

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
                usuario.getCpf(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getDataCadastro()
        );
    }
}
