package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class TwelveDataCotacaoProvider implements CotacaoProvider {

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataCotacaoProvider(@Value("${twelvedata.api.key:demo}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.twelvedata.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean suporta(Mercado mercado) {
        return mercado == Mercado.EUA;
    }

    @Override
    @CircuitBreaker(name = "cotacaoEua", fallbackMethod = "buscarCotacaoFallback")
    public DadosCotacaoResponse buscarCotacao(String ticker) {
        try {
            RespostaTwelveData resposta = RetryExterno.tentar(3, 300, () -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(RespostaTwelveData.class));

            if (resposta == null || resposta.close() == null) {
                throw new RecursoNaoEncontradoException("Ticker nao encontrado na Twelve Data: " + ticker);
            }

            return new DadosCotacaoResponse(
                    resposta.symbol(),
                    resposta.name(),
                    resposta.currency(),
                    new BigDecimal(resposta.close()),
                    Instant.ofEpochSecond(resposta.timestamp()).atOffset(ZoneOffset.UTC).toLocalDateTime(),
                    // Padrao publico e sem chave da FMP -- o endpoint /logo da propria
                    // Twelve Data se mostrou instavel (404 em tickers validos).
                    "https://financialmodelingprep.com/image-stock/" + resposta.symbol() + ".png"
            );
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServicoExternoIndisponivelException(
                    "Twelve Data recusou a chave de API. Configure TWELVEDATA_API_KEY com uma chave valida.");
        } catch (HttpClientErrorException ex) {
            throw new RecursoNaoEncontradoException("Ticker nao encontrado na Twelve Data: " + ticker);
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar a cotacao na Twelve Data");
        }
    }

    private DadosCotacaoResponse buscarCotacaoFallback(String ticker, Throwable t) {
        throw new ServicoExternoIndisponivelException("Twelve Data indisponivel no momento (circuit breaker aberto)");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaTwelveData(
            String symbol,
            String name,
            String currency,
            String close,
            Long timestamp
    ) {
    }
}
