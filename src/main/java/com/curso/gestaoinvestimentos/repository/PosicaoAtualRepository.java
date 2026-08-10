package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosicaoAtualRepository extends JpaRepository<PosicaoAtual, Long> {

    Optional<PosicaoAtual> findByCarteiraIdAndAcaoIdAndCorretoraId(Long carteiraId, Long acaoId, Long corretoraId);

    List<PosicaoAtual> findByCarteiraId(Long carteiraId);

    void deleteByCarteiraId(Long carteiraId);
}
