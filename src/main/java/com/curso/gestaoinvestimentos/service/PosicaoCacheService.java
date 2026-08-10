package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantem PosicaoAtual (cache materializado) sincronizado com o historico de
 * Operacao, que continua sendo a unica fonte da verdade. atualizar() e
 * chamado no mesmo commit de OperacaoService.registrar/cancelar --
 * reconstruirCarteira() e a rede de seguranca caso cache e historico algum
 * dia divirjam.
 */
@Service
public class PosicaoCacheService {

    private final OperacaoRepository operacaoRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;

    public PosicaoCacheService(OperacaoRepository operacaoRepository, PosicaoAtualRepository posicaoAtualRepository) {
        this.operacaoRepository = operacaoRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
    }

    public void atualizar(Carteira carteira, Acao acao, Corretora corretora) {
        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                carteira.getId(), acao.getId(), corretora.getId(), StatusOperacao.ATIVA);
        PosicaoCalculator.Posicao calculada = PosicaoCalculator.calcular(historico);

        PosicaoAtual posicaoAtual = posicaoAtualRepository
                .findByCarteiraIdAndAcaoIdAndCorretoraId(carteira.getId(), acao.getId(), corretora.getId())
                .orElseGet(PosicaoAtual::new);
        posicaoAtual.setCarteira(carteira);
        posicaoAtual.setAcao(acao);
        posicaoAtual.setCorretora(corretora);
        posicaoAtual.setQuantidade(calculada.quantidade());
        posicaoAtual.setPrecoMedio(calculada.precoMedio());
        posicaoAtual.setAtualizadoEm(LocalDateTime.now());

        posicaoAtualRepository.save(posicaoAtual);
    }

    public void reconstruirCarteira(Carteira carteira) {
        posicaoAtualRepository.deleteByCarteiraId(carteira.getId());

        List<Operacao> ativas = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteira.getId(), StatusOperacao.ATIVA);

        Map<String, List<Operacao>> porAcaoECorretora = new LinkedHashMap<>();
        for (Operacao operacao : ativas) {
            String chave = operacao.getAcao().getId() + "-" + operacao.getCorretora().getId();
            porAcaoECorretora.computeIfAbsent(chave, k -> new ArrayList<>()).add(operacao);
        }

        for (List<Operacao> grupo : porAcaoECorretora.values()) {
            Operacao qualquerOperacaoDoGrupo = grupo.get(0);
            atualizar(carteira, qualquerOperacaoDoGrupo.getAcao(), qualquerOperacaoDoGrupo.getCorretora());
        }
    }
}
