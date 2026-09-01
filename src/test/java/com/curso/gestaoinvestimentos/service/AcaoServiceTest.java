package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.AcaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.curso.gestaoinvestimentos.integration.CotacaoProvider;
import com.curso.gestaoinvestimentos.integration.DadosCotacaoResponse;
import com.curso.gestaoinvestimentos.integration.DolarApiCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock
    private AcaoRepository repository;
    @Mock
    private HistoricoCotacaoRepository historicoRepository;
    @Mock
    private OperacaoRepository operacaoRepository;
    @Mock
    private PosicaoAtualRepository posicaoAtualRepository;
    @Mock
    private CotacaoProvider cotacaoProvider;
    @Mock
    private DolarApiCambioClient cambioClient;

    private AcaoService service;

    @BeforeEach
    void setUp() {
        service = new AcaoService(repository, historicoRepository, operacaoRepository, posicaoAtualRepository,
                List.of(cotacaoProvider), cambioClient);
    }

    private Acao acaoEua(String ticker, String cotacao) {
        Acao acao = new Acao();
        acao.setId(1L);
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.EUA);
        acao.setMoeda("USD");
        acao.setCotacaoAtual(new BigDecimal(cotacao));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private Acao acaoBrasil(String ticker) {
        Acao acao = new Acao();
        acao.setId(2L);
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("30.00"));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Test
    void listarConverteCotacaoDeAcaoEuaParaReais() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(acaoEua("AAPL", "310.49"))));
        when(cambioClient.buscarTaxaUsdParaBrl()).thenReturn(new BigDecimal("5.21"));

        List<AcaoResponseDTO> resultado = service.listar(PAGEABLE).getContent();

        // 310.49 * 5.21 = 1617.6529, arredondado pra 1617.65
        assertEquals(0, resultado.get(0).cotacaoAtualBRL().compareTo(new BigDecimal("1617.65")));
    }

    @Test
    void listarNaoConverteCotacaoDeAcaoBrasil() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(acaoBrasil("PETR4"))));

        List<AcaoResponseDTO> resultado = service.listar(PAGEABLE).getContent();

        assertNull(resultado.get(0).cotacaoAtualBRL());
    }

    @Test
    void listarDegradaGraciosamenteQuandoCambioFalha() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(acaoEua("AAPL", "310.49"))));
        when(cambioClient.buscarTaxaUsdParaBrl()).thenThrow(new ServicoExternoIndisponivelException("indisponivel"));

        List<AcaoResponseDTO> resultado = service.listar(PAGEABLE).getContent();

        assertNull(resultado.get(0).cotacaoAtualBRL());
        assertEquals(0, resultado.get(0).cotacaoAtual().compareTo(new BigDecimal("310.49")));
    }

    @Test
    void excluirRemoveAcaoESeuHistoricoQuandoSemUso() {
        when(repository.existsById(1L)).thenReturn(true);
        when(operacaoRepository.existsByAcaoId(1L)).thenReturn(false);
        when(posicaoAtualRepository.existsByAcaoId(1L)).thenReturn(false);

        service.excluir(1L);

        verify(historicoRepository).deleteByAcaoId(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void excluirLancaExcecaoQuandoAcaoNaoExiste() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> service.excluir(1L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void excluirBloqueiaQuandoExistemOperacoes() {
        when(repository.existsById(1L)).thenReturn(true);
        when(operacaoRepository.existsByAcaoId(1L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.excluir(1L));
        verify(repository, never()).deleteById(any());
        verify(historicoRepository, never()).deleteByAcaoId(any());
    }

    @Test
    void excluirBloqueiaQuandoExistemPosicoes() {
        when(repository.existsById(1L)).thenReturn(true);
        when(operacaoRepository.existsByAcaoId(1L)).thenReturn(false);
        when(posicaoAtualRepository.existsByAcaoId(1L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.excluir(1L));
        verify(repository, never()).deleteById(any());
        verify(historicoRepository, never()).deleteByAcaoId(any());
    }

    @Test
    void criarRejeitaTickerEuaCadastradoComoBrasil() {
        when(repository.findByTicker("INTC")).thenReturn(java.util.Optional.empty());
        when(cotacaoProvider.suporta(Mercado.BRASIL)).thenReturn(true);
        when(cotacaoProvider.buscarCotacao("INTC")).thenReturn(new DadosCotacaoResponse(
                "INTC", "Intel Corporation", "USD", new BigDecimal("89.51"), LocalDateTime.now(), null));

        AcaoRequestDTO dto = new AcaoRequestDTO("INTC", Mercado.BRASIL);

        assertThrows(RegraDeNegocioException.class, () -> service.criar(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void criarRejeitaTickerBrasilCadastradoComoEua() {
        when(repository.findByTicker("PETR4")).thenReturn(java.util.Optional.empty());
        when(cotacaoProvider.suporta(Mercado.EUA)).thenReturn(true);
        when(cotacaoProvider.buscarCotacao("PETR4")).thenReturn(new DadosCotacaoResponse(
                "PETR4", "Petroleo Brasileiro SA", "BRL", new BigDecimal("45.02"), LocalDateTime.now(), null));

        AcaoRequestDTO dto = new AcaoRequestDTO("PETR4", Mercado.EUA);

        assertThrows(RegraDeNegocioException.class, () -> service.criar(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void criarAceitaQuandoMoedaCorrespondeAoMercado() {
        when(repository.findByTicker("AAPL")).thenReturn(java.util.Optional.empty());
        when(cotacaoProvider.suporta(Mercado.EUA)).thenReturn(true);
        when(cotacaoProvider.buscarCotacao("AAPL")).thenReturn(new DadosCotacaoResponse(
                "AAPL", "Apple Inc.", "USD", new BigDecimal("316.85"), LocalDateTime.now(), null));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcaoRequestDTO dto = new AcaoRequestDTO("AAPL", Mercado.EUA);

        service.criar(dto);

        verify(repository).save(any());
    }
}
