package com.curso.gestaoinvestimentos.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                           CarteiraRepository carteiraRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.carteiraRepository = carteiraRepository;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        repository.findByEmail(dto.email()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email " + dto.email());
        });

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        // Campos controlados pelo sistema, nunca pelo cliente:
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());

        Usuario salvo = repository.save(usuario);

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteiraRepository.save(carteira);

        return toResponseDTO(salvo);
    }

    public List<UsuarioResponseDTO> listar() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
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
