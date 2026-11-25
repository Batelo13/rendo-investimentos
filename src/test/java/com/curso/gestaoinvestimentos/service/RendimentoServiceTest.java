package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.RendimentoPontoDTO;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.HistoricoCotacao;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aula 15: serie de rendimento (realizado + nao realizado) por ponto no tempo.
 * Mocks so nos repositorios (dependencias de banco); o calculo e real.
 */
@ExtendWith(MockitoExtension.class)
class RendimentoServiceTest {

    private static final Long CARTEIRA = 1L;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 10, 10, 0);

    @Mock
    private OperacaoRepository operacaoRepository;
    @Mock
    private HistoricoCotacaoRepository historicoRepository;
    @InjectMocks
    private RendimentoService service;

    private final Acao acao = acao(7L);
    private final Corretora corretora = corretora(3L);

    private static Acao acao(Long id) {
        Acao a = new Acao();
        a.setId(id);
        return a;
    }

    private static Corretora corretora(Long id) {
        Corretora c = new Corretora();
        c.setId(id);
        return c;
    }

    private Operacao op(TipoOperacao tipo, String qtd, String preco, LocalDateTime quando, String precoMedioNaVenda) {
        Operacao o = new Operacao();
        o.setAcao(acao);
        o.setCorretora(corretora);
        o.setTipo(tipo);
        o.setQuantidade(new BigDecimal(qtd));
        o.setPrecoUnitario(new BigDecimal(preco));
        o.setDataHora(quando);
        o.setPrecoMedioNaVenda(precoMedioNaVenda == null ? null : new BigDecimal(precoMedioNaVenda));
        return o;
    }

    private HistoricoCotacao cotacao(String preco, LocalDateTime quando) {
        HistoricoCotacao h = new HistoricoCotacao();
        h.setAcao(acao);
        h.setPreco(new BigDecimal(preco));
        h.setCapturadoEm(quando);
        return h;
    }

    private void dado(List<Operacao> operacoes, List<HistoricoCotacao> historicoDesc) {
        when(operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(CARTEIRA, StatusOperacao.ATIVA))
                .thenReturn(operacoes);
        when(historicoRepository.findByAcaoIdOrderByCapturadoEmDesc(7L)).thenReturn(historicoDesc);
    }

    private static BigDecimal valor(List<RendimentoPontoDTO> serie, int i) {
        return serie.get(i).rendimento();
    }

    @Test
    void carteiraSemOperacoesNaoTemSerieNemConsultaCotacoes() {
        when(operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(CARTEIRA, StatusOperacao.ATIVA))
                .thenReturn(List.of());

        assertThat(service.calcularSerie(CARTEIRA)).isEmpty();
        verify(historicoRepository, never()).findByAcaoIdOrderByCapturadoEmDesc(anyLong());
    }

    @Test
    void compraSemCotacaoAnteriorValeZeroEValorizaQuandoChegaCotacao() {
        dado(List.of(op(TipoOperacao.COMPRA, "10", "100", T0, null)),
                List.of(cotacao("120", T0.plusDays(1))));

        List<RendimentoPontoDTO> serie = service.calcularSerie(CARTEIRA);

        assertThat(serie).extracting(RendimentoPontoDTO::timestamp).containsExactly(T0, T0.plusDays(1));
        assertThat(valor(serie, 0)).isEqualByComparingTo("0");     // sem preco conhecido ainda
        assertThat(valor(serie, 1)).isEqualByComparingTo("200");   // (120 - 100) x 10
    }

    @Test
    void usaACotacaoMaisRecenteAteOMomentoDaCompra() {
        // historico DESC: o ponto de T0-1h e o preco vigente na compra.
        dado(List.of(op(TipoOperacao.COMPRA, "10", "100", T0, null)),
                List.of(cotacao("90", T0.minusHours(1)), cotacao("80", T0.minusDays(1))));

        List<RendimentoPontoDTO> serie = service.calcularSerie(CARTEIRA);

        // Antes da compra nao ha posicao: rendimento 0. Na compra: (90 - 100) x 10.
        assertThat(serie).hasSize(3);
        assertThat(valor(serie, 0)).isEqualByComparingTo("0");
        assertThat(valor(serie, 1)).isEqualByComparingTo("0");
        assertThat(valor(serie, 2)).isEqualByComparingTo("-100");
    }

    @Test
    void vendaTotalViraLucroRealizadoESemPosicaoNaoRealizado() {
        dado(List.of(op(TipoOperacao.COMPRA, "10", "100", T0, null),
                        op(TipoOperacao.VENDA, "10", "150", T0.plusDays(2), "100")),
                List.of(cotacao("500", T0.plusDays(3))));

        List<RendimentoPontoDTO> serie = service.calcularSerie(CARTEIRA);

        // Depois da venda total, a cotacao alta nao conta: so o realizado (150 - 100) x 10.
        assertThat(valor(serie, 1)).isEqualByComparingTo("500");
        assertThat(valor(serie, 2)).isEqualByComparingTo("500");
    }

    @Test
    void vendaParcialSomaRealizadoENaoRealizado() {
        dado(List.of(op(TipoOperacao.COMPRA, "10", "100", T0, null),
                        op(TipoOperacao.VENDA, "4", "110", T0.plusDays(1), "100")),
                List.of(cotacao("130", T0.plusDays(2))));

        List<RendimentoPontoDTO> serie = service.calcularSerie(CARTEIRA);

        // realizado (110-100)x4 = 40; nao realizado (130-100)x6 = 180.
        assertThat(valor(serie, 2)).isEqualByComparingTo("220");
    }

    @Test
    void vendaSemPrecoMedioRegistradoNaoEntraNoRealizado() {
        dado(List.of(op(TipoOperacao.COMPRA, "10", "100", T0, null),
                        op(TipoOperacao.VENDA, "10", "150", T0.plusDays(1), null)),
                List.of());

        List<RendimentoPontoDTO> serie = service.calcularSerie(CARTEIRA);

        assertThat(valor(serie, 1)).isEqualByComparingTo("0");
    }
}
