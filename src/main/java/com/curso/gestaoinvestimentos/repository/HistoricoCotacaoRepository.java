package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.HistoricoCotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoCotacaoRepository extends JpaRepository<HistoricoCotacao, Long> {

    List<HistoricoCotacao> findByAcaoIdOrderByCapturadoEmDesc(Long acaoId);
}
