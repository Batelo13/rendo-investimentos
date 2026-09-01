package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    Page<Operacao> findByCarteiraIdOrderByDataHoraDesc(Long carteiraId, Pageable pageable);

    List<Operacao> findByCarteiraIdAndStatusOrderByDataHoraAsc(Long carteiraId, StatusOperacao status);

    List<Operacao> findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
            Long carteiraId, Long acaoId, Long corretoraId, StatusOperacao status);

    boolean existsByAcaoId(Long acaoId);
}
