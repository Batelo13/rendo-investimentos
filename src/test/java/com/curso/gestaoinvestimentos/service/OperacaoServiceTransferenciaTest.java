package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.TransferenciaRequestDTO;
import com.curso.gestaoinvestimentos.dto.TransferenciaResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.integration.DolarApiCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Aula 15: transferencia de custodia entre corretoras. Mocks nos repositorios,
 * no cache de posicao e no cliente de cambio (rede externa).
 * Lenient: o setup comum prepara o caminho feliz; os cenarios de erro param antes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperacaoServiceTransferenciaTest {

    private static final String EMAIL = "dono@example.com";

    @Mock private OperacaoRepository operacaoRepository;
    @Mock private CarteiraRepository carteiraRepository;
    @Mock private AcaoRepository acaoRepository;
    @Mock private CorretoraRepository corretoraRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PosicaoCacheService posicaoCacheService;
    @Mock private DolarApiCambioClient cambioClient;
    @InjectMocks private OperacaoService service;

    private final Carteira carteira = new Carteira();
    private final Acao petr4 = acao(1L, "PETR4", Mercado.BRASIL);
    private final Corretora origem = corretora(10L, "Origem", true);
    private final Corretora destino = corretora(20L, "Destino", true);

    private static Acao acao(Long id, String ticker, Mercado mercado) {
        Acao a = new Acao();
        a.setId(id);
        a.setTicker(ticker);
        a.setMercado(mercado);
        return a;
    }

    private static Corretora corretora(Long id, String nome, boolean cvm) {
        Corretora c = new Corretora();
        c.setId(id);
        c.setNomeFantasia(nome);
        c.setValidadaNaCvm(cvm);
        return c;
    }

    private Operacao compra(Acao acao, String qtd, String preco) {
        Operacao o = new Operacao();
        o.setAcao(acao);
        o.setCorretora(origem);
        o.setTipo(TipoOperacao.COMPRA);
        o.setQuantidade(new BigDecimal(qtd));
        o.setPrecoUnitario(new BigDecimal(preco));
        o.setDataHora(LocalDateTime.now().minusDays(1));
        o.setStatus(StatusOperacao.ATIVA);
        return o;
    }

    @BeforeEach
    void caminhoFeliz() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        carteira.setId(50L);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carteiraRepository.findByUsuarioId(5L)).thenReturn(Optional.of(carteira));
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(petr4));
        when(corretoraRepository.findById(10L)).thenReturn(Optional.of(origem));
        when(corretoraRepository.findById(20L)).thenReturn(Optional.of(destino));
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(50L, 1L, 10L, StatusOperacao.ATIVA))
                .thenReturn(List.of(compra(petr4, "10", "30.00")));
        when(operacaoRepository.save(any(Operacao.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TransferenciaRequestDTO pedido(Long acaoId, String qtd) {
        return new TransferenciaRequestDTO(acaoId, 10L, 20L, new BigDecimal(qtd));
    }

    @Test
    void transfereAoPrecoMedioSemMexerNoCambioEmAcaoBrasileira() {
        TransferenciaResponseDTO r = service.transferir(EMAIL, pedido(1L, "4"));

        assertThat(r.saida().tipo()).isEqualTo(TipoOperacao.TRANSFERENCIA_SAIDA);
        assertThat(r.saida().corretoraNome()).isEqualTo("Origem");
        assertThat(r.entrada().tipo()).isEqualTo(TipoOperacao.TRANSFERENCIA_ENTRADA);
        assertThat(r.entrada().corretoraNome()).isEqualTo("Destino");
        assertThat(r.entrada().precoUnitario()).isEqualByComparingTo("30.00");
        assertThat(r.entrada().taxaCambio()).isEqualByComparingTo("1");
        verifyNoInteractions(cambioClient);
        verify(posicaoCacheService).atualizar(carteira, petr4, origem);
        verify(posicaoCacheService).atualizar(carteira, petr4, destino);
    }

    @Test
    void podeTransferirExatamenteOSaldoDaOrigem() {
        assertThat(service.transferir(EMAIL, pedido(1L, "10")).saida().quantidade()).isEqualByComparingTo("10");
    }

    @Test
    void acaoAmericanaGravaATaxaDeCambioDoMomento() {
        Acao aapl = acao(2L, "AAPL", Mercado.EUA);
        when(acaoRepository.findById(2L)).thenReturn(Optional.of(aapl));
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(50L, 2L, 10L, StatusOperacao.ATIVA))
                .thenReturn(List.of(compra(aapl, "1", "200.00")));
        when(cambioClient.buscarTaxaUsdParaBrl()).thenReturn(new BigDecimal("5.40"));

        TransferenciaResponseDTO r = service.transferir(EMAIL, pedido(2L, "0.5"));

        assertThat(r.saida().taxaCambio()).isEqualByComparingTo("5.40");
        assertThat(r.entrada().quantidade()).isEqualByComparingTo("0.5");
    }

    @Test
    void recusaOrigemIgualAoDestinoSemConsultarNada() {
        assertThatThrownBy(() -> service.transferir(EMAIL, new TransferenciaRequestDTO(1L, 10L, 10L, BigDecimal.ONE)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("diferentes");
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void recusaQuantidadeAcimaDoSaldoNaOrigem() {
        assertThatThrownBy(() -> service.transferir(EMAIL, pedido(1L, "11")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Saldo insuficiente");
        verify(operacaoRepository, never()).save(any());
    }

    @Test
    void recusaFracaoDeAcaoBrasileira() {
        assertThatThrownBy(() -> service.transferir(EMAIL, pedido(1L, "1.5")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fracionaria");
    }

    @Test
    void recusaDestinoNaoValidadoNaCvm() {
        destino.setValidadaNaCvm(false);

        assertThatThrownBy(() -> service.transferir(EMAIL, pedido(1L, "1")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CVM");
    }

    @Test
    void acaoInexistenteVira404() {
        assertThatThrownBy(() -> service.transferir(EMAIL, pedido(99L, "1")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Acao nao encontrada");
    }

    @Test
    void corretoraDeDestinoInexistenteVira404() {
        when(corretoraRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferir(EMAIL, pedido(1L, "1")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("destino");
    }
}
