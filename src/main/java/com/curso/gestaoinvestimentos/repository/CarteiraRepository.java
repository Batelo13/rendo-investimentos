package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {

    Optional<Carteira> findByUsuarioId(Long usuarioId);
}
