package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.AcaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.HistoricoCotacaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.integration.CotacaoProvider;
import com.curso.gestaoinvestimentos.integration.DadosCotacaoResponse;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.HistoricoCotacao;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcaoService {

    private final AcaoRepository repository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final List<CotacaoProvider> providers;

    public AcaoService(AcaoRepository repository, HistoricoCotacaoRepository historicoRepository, List<CotacaoProvider> providers) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.providers = providers;
    }

    public AcaoResponseDTO criar(AcaoRequestDTO dto) {
        repository.findByTicker(dto.ticker()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe uma acao cadastrada com o ticker " + dto.ticker());
        });

        DadosCotacaoResponse dadosCotacao = buscarCotacao(dto.ticker(), dto.mercado());

        Acao acao = new Acao();
        acao.setTicker(dadosCotacao.ticker());
        acao.setNomeEmpresa(dadosCotacao.nomeEmpresa());
        acao.setMercado(dto.mercado());
        acao.setMoeda(dadosCotacao.moeda());
        acao.setCotacaoAtual(dadosCotacao.cotacaoAtual());
        acao.setDataHoraCotacao(dadosCotacao.dataHoraCotacao());

        Acao salva = repository.save(acao);
        registrarHistorico(salva, dadosCotacao);
        return toResponseDTO(salva);
    }

    public List<AcaoResponseDTO> listar() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AcaoResponseDTO buscarPorId(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + id));
        return toResponseDTO(acao);
    }

    public AcaoResponseDTO buscarPorTicker(String ticker) {
        Acao acao = repository.findByTicker(ticker)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com ticker " + ticker));
        return toResponseDTO(acao);
    }

    public AcaoResponseDTO atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + id));

        DadosCotacaoResponse dadosCotacao = buscarCotacao(acao.getTicker(), acao.getMercado());
        acao.setCotacaoAtual(dadosCotacao.cotacaoAtual());
        acao.setDataHoraCotacao(dadosCotacao.dataHoraCotacao());

        Acao salva = repository.save(acao);
        registrarHistorico(salva, dadosCotacao);
        return toResponseDTO(salva);
    }

    public List<HistoricoCotacaoResponseDTO> historico(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Acao nao encontrada com id " + id);
        }
        return historicoRepository.findByAcaoIdOrderByCapturadoEmDesc(id).stream()
                .map(h -> new HistoricoCotacaoResponseDTO(h.getPreco(), h.getCapturadoEm()))
                .toList();
    }

    private void registrarHistorico(Acao acao, DadosCotacaoResponse dadosCotacao) {
        HistoricoCotacao historico = new HistoricoCotacao();
        historico.setAcao(acao);
        historico.setPreco(dadosCotacao.cotacaoAtual());
        historico.setCapturadoEm(dadosCotacao.dataHoraCotacao());
        historicoRepository.save(historico);
    }

    /**
     * Selecao do Strategy: percorre os providers disponiveis e usa o primeiro
     * que declarar suporte ao mercado pedido. Adicionar um mercado novo no
     * futuro significa criar um CotacaoProvider novo, sem alterar este metodo.
     */
    private DadosCotacaoResponse buscarCotacao(String ticker, Mercado mercado) {
        CotacaoProvider provider = providers.stream()
                .filter(p -> p.suporta(mercado))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum provedor de cotacao disponivel para o mercado " + mercado));

        return provider.buscarCotacao(ticker);
    }

    private AcaoResponseDTO toResponseDTO(Acao acao) {
        return new AcaoResponseDTO(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                acao.getCotacaoAtual(),
                acao.getDataHoraCotacao()
        );
    }
}
