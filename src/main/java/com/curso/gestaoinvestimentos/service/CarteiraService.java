package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final OperacaoRepository operacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarteiraService(CarteiraRepository carteiraRepository, OperacaoRepository operacaoRepository,
                            UsuarioRepository usuarioRepository) {
        this.carteiraRepository = carteiraRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PosicaoDTO> buscarPosicaoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        return buscarPosicaoPorUsuarioId(usuario.getId());
    }

    public List<PosicaoDTO> buscarPosicaoPorUsuarioId(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));

        List<Operacao> ativas = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteira.getId(), StatusOperacao.ATIVA);

        Map<String, List<Operacao>> porAcaoECorretora = new LinkedHashMap<>();
        for (Operacao operacao : ativas) {
            String chave = operacao.getAcao().getId() + "-" + operacao.getCorretora().getId();
            porAcaoECorretora.computeIfAbsent(chave, k -> new ArrayList<>()).add(operacao);
        }

        List<PosicaoDTO> posicoes = new ArrayList<>();
        for (List<Operacao> grupo : porAcaoECorretora.values()) {
            PosicaoCalculator.Posicao calculada = PosicaoCalculator.calcular(grupo);
            if (calculada.quantidade() <= 0) {
                continue;
            }
            Acao acao = grupo.get(0).getAcao();
            Corretora corretora = grupo.get(0).getCorretora();
            BigDecimal valorInvestido = calculada.precoMedio().multiply(BigDecimal.valueOf(calculada.quantidade()));
            BigDecimal valorAtual = acao.getCotacaoAtual() == null
                    ? null
                    : acao.getCotacaoAtual().multiply(BigDecimal.valueOf(calculada.quantidade()));

            posicoes.add(new PosicaoDTO(
                    acao.getTicker(),
                    corretora.getNomeFantasia(),
                    calculada.quantidade(),
                    calculada.precoMedio(),
                    valorInvestido,
                    valorAtual
            ));
        }
        return posicoes;
    }
}
