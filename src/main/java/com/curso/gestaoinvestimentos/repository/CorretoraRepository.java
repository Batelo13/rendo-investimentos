package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    Optional<Corretora> findByCnpj(String cnpj);
}
