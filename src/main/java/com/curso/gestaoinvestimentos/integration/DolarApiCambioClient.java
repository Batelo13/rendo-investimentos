package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Isola a busca da taxa de cambio USD->BRL via dolarapi.com (publica, sem
 * chave de API). Sem interface Strategy aqui -- diferente de CotacaoProvider
 * (que genuinamente escolhe entre BRASIL/EUA), so existe uma fonte de
 * cambio, entao uma interface seria abstracao sem necessidade.
 */
@Component
public class DolarApiCambioClient {

    private final RestClient restClient;

    public DolarApiCambioClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://br.dolarapi.com/v1")
                .requestFactory(requestFactory)
                .build();
    }

    @CircuitBreaker(name = "cambio", fallbackMethod = "buscarTaxaUsdParaBrlFallback")
    public BigDecimal buscarTaxaUsdParaBrl() {
        try {
            RespostaCotacao resposta = RetryExterno.tentar(3, 300, () -> restClient.get()
                    .uri("/cotacoes/usd")
                    .retrieve()
                    .body(RespostaCotacao.class));

            if (resposta == null || resposta.venda() == null) {
                throw new ServicoExternoIndisponivelException("Nao foi possivel obter a cotacao USD/BRL na dolarapi");
            }

            return resposta.venda();
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar a cotacao USD/BRL na dolarapi");
        }
    }

    private BigDecimal buscarTaxaUsdParaBrlFallback(Throwable t) {
        throw new ServicoExternoIndisponivelException("dolarapi indisponivel no momento (circuit breaker aberto)");
    }

    // "venda" -- taxa pela qual se adquire dolar -- e o custo real de converter
    // BRL em USD para uma compra, mais realista que "compra" pra esse uso.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaCotacao(BigDecimal compra, BigDecimal venda) {
    }
}
