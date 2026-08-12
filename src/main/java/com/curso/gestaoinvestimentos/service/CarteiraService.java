package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.dto.RendimentoPontoDTO;
import com.curso.gestaoinvestimentos.dto.SaldoDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
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
    private final OperacaoRepository operacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PosicaoCacheService posicaoCacheService;
    private final RendimentoService rendimentoService;

    public CarteiraService(CarteiraRepository carteiraRepository, PosicaoAtualRepository posicaoAtualRepository,
                            OperacaoRepository operacaoRepository, UsuarioRepository usuarioRepository,
                            PosicaoCacheService posicaoCacheService, RendimentoService rendimentoService) {
        this.carteiraRepository = carteiraRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
        this.rendimentoService = rendimentoService;
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

    // Sem cache aqui, diferente da posicao: e uma unica soma sobre o historico
    // inteiro da carteira (nao por acao+corretora), bem mais barato de calcular
    // na hora. Se um dia isso precisar de cache, mesmo padrao do PosicaoAtual.
    public SaldoDTO buscarSaldoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));

        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteira.getId(), StatusOperacao.ATIVA);
        BigDecimal saldoDisponivel = SaldoCalculator.calcular(carteira.getSaldoInicial(), historico);

        return new SaldoDTO(carteira.getSaldoInicial(), saldoDisponivel);
    }

    public List<RendimentoPontoDTO> buscarRendimentoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));

        return rendimentoService.calcularSerie(carteira.getId());
    }
}
