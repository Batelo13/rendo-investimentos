package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    long countByAtivo(Boolean ativo);

    long countByEmailVerified(Boolean emailVerified);

    long countByRole(Role role);

    long countByDataCadastroGreaterThanEqual(LocalDate data);
}
