package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;
    private final UsuarioRepository usuarioRepository;
    private final PosicaoCacheService posicaoCacheService;

    public CarteiraService(CarteiraRepository carteiraRepository, PosicaoAtualRepository posicaoAtualRepository,
                            UsuarioRepository usuarioRepository, PosicaoCacheService posicaoCacheService) {
        this.carteiraRepository = carteiraRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
    }

    public List<PosicaoDTO> buscarPosicaoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        return buscarPosicaoPorUsuarioId(usuario.getId());
    }

    public List<PosicaoDTO> buscarPosicaoPorUsuarioId(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));

        List<PosicaoAtual> posicoesEmCache = posicaoAtualRepository.findByCarteiraId(carteira.getId());

        List<PosicaoDTO> posicoes = new ArrayList<>();
        for (PosicaoAtual posicaoAtual : posicoesEmCache) {
            if (posicaoAtual.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Acao acao = posicaoAtual.getAcao();
            Corretora corretora = posicaoAtual.getCorretora();
            BigDecimal valorInvestido = posicaoAtual.getPrecoMedio().multiply(posicaoAtual.getQuantidade());
            BigDecimal valorAtual = acao.getCotacaoAtual() == null
                    ? null
                    : acao.getCotacaoAtual().multiply(posicaoAtual.getQuantidade());

            posicoes.add(new PosicaoDTO(
                    acao.getTicker(),
                    corretora.getNomeFantasia(),
                    posicaoAtual.getQuantidade(),
                    posicaoAtual.getPrecoMedio(),
                    valorInvestido,
                    valorAtual
            ));
        }
        return posicoes;
    }

    public List<PosicaoDTO> reconstruirPosicao(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));
        posicaoCacheService.reconstruirCarteira(carteira);
        return buscarPosicaoPorUsuarioId(usuarioId);
    }
}
