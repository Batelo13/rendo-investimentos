package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.RendimentoPontoDTO;
import com.curso.gestaoinvestimentos.model.HistoricoCotacao;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reconstroi o rendimento total (realizado + nao realizado) da carteira em
 * pontos passados no tempo, reaproveitando PosicaoCalculator/SaldoCalculator
 * (funcoes puras que ja aceitam qualquer subconjunto ordenado de Operacao)
 * e o HistoricoCotacao pra saber o preco de cada acao em cada momento.
 * Amostra so nos timestamps onde ha dado real (operacao ou cotacao capturada)
 * -- sem interpolar entre pontos que nunca existiram de fato.
 */
@Service
public class RendimentoService {

    private record ChaveGrupo(Long acaoId, Long corretoraId) {
    }

    private final OperacaoRepository operacaoRepository;
    private final HistoricoCotacaoRepository historicoCotacaoRepository;

    public RendimentoService(OperacaoRepository operacaoRepository, HistoricoCotacaoRepository historicoCotacaoRepository) {
        this.operacaoRepository = operacaoRepository;
        this.historicoCotacaoRepository = historicoCotacaoRepository;
    }

    public List<RendimentoPontoDTO> calcularSerie(Long carteiraId) {
        List<Operacao> operacoes = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteiraId, StatusOperacao.ATIVA);
        if (operacoes.isEmpty()) {
            return List.of();
        }

        Set<Long> acaoIds = operacoes.stream().map(o -> o.getAcao().getId()).collect(Collectors.toSet());
        Map<Long, List<HistoricoCotacao>> historicoPorAcao = new HashMap<>();
        for (Long acaoId : acaoIds) {
            historicoPorAcao.put(acaoId, historicoCotacaoRepository.findByAcaoIdOrderByCapturadoEmDesc(acaoId));
        }

        TreeSet<LocalDateTime> timestamps = new TreeSet<>();
        for (Operacao operacao : operacoes) {
            timestamps.add(operacao.getDataHora());
        }
        for (List<HistoricoCotacao> historico : historicoPorAcao.values()) {
            for (HistoricoCotacao ponto : historico) {
                timestamps.add(ponto.getCapturadoEm());
            }
        }

        List<RendimentoPontoDTO> serie = new ArrayList<>();
        for (LocalDateTime t : timestamps) {
            serie.add(new RendimentoPontoDTO(t, calcularRendimentoEm(operacoes, historicoPorAcao, t)));
        }
        return serie;
    }

    private BigDecimal calcularRendimentoEm(List<Operacao> operacoes, Map<Long, List<HistoricoCotacao>> historicoPorAcao, LocalDateTime t) {
        List<Operacao> ateT = operacoes.stream().filter(o -> !o.getDataHora().isAfter(t)).toList();

        Map<ChaveGrupo, List<Operacao>> grupos = ateT.stream()
                .collect(Collectors.groupingBy(o -> new ChaveGrupo(o.getAcao().getId(), o.getCorretora().getId())));

        BigDecimal naoRealizado = BigDecimal.ZERO;
        for (Map.Entry<ChaveGrupo, List<Operacao>> grupo : grupos.entrySet()) {
            PosicaoCalculator.Posicao posicao = PosicaoCalculator.calcular(grupo.getValue());
            if (posicao.quantidade().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal precoMercado = precoEm(historicoPorAcao.get(grupo.getKey().acaoId()), t);
            if (precoMercado == null) {
                continue;
            }

            BigDecimal valorAtual = precoMercado.multiply(posicao.quantidade());
            BigDecimal valorInvestido = posicao.precoMedio().multiply(posicao.quantidade());
            naoRealizado = naoRealizado.add(valorAtual).subtract(valorInvestido);
        }

        BigDecimal realizado = ateT.stream()
                .filter(o -> o.getTipo() == TipoOperacao.VENDA && o.getPrecoMedioNaVenda() != null)
                .map(o -> o.getPrecoUnitario().subtract(o.getPrecoMedioNaVenda()).multiply(o.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return realizado.add(naoRealizado);
    }

    /**
     * historico vem ordenado capturadoEm DESC (mesmo finder usado no endpoint
     * de leitura) -- o primeiro ponto com capturadoEm <= t e o preco mais
     * recente conhecido naquele momento.
     */
    private BigDecimal precoEm(List<HistoricoCotacao> historicoDesc, LocalDateTime t) {
        if (historicoDesc == null) {
            return null;
        }
        for (HistoricoCotacao ponto : historicoDesc) {
            if (!ponto.getCapturadoEm().isAfter(t)) {
                return ponto.getPreco();
            }
        }
        return null;
    }
}
