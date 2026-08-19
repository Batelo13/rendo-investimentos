package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Isola a busca da taxa de cambio USD->BRL, mesma API/chave ja usada pra
 * cotacao de acoes EUA (TwelveDataCotacaoProvider). Sem interface Strategy
 * aqui -- diferente de CotacaoProvider (que genuinamente escolhe entre
 * BRASIL/EUA), so existe uma fonte de cambio, entao uma interface seria
 * abstracao sem necessidade.
 */
@Component
public class TwelveDataCambioClient {

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataCambioClient(@Value("${twelvedata.api.key:demo}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.twelvedata.com")
                .requestFactory(requestFactory)
                .build();
    }

    public BigDecimal buscarTaxaUsdParaBrl() {
        try {
            RespostaExchangeRate resposta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/exchange_rate")
                            .queryParam("symbol", "USD/BRL")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(RespostaExchangeRate.class);

            if (resposta == null || resposta.rate() == null) {
                throw new ServicoExternoIndisponivelException("Nao foi possivel obter a cotacao USD/BRL na Twelve Data");
            }

            return resposta.rate();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServicoExternoIndisponivelException(
                    "Twelve Data recusou a chave de API. Configure TWELVEDATA_API_KEY com uma chave valida.");
        } catch (HttpClientErrorException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel obter a cotacao USD/BRL na Twelve Data");
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar a cotacao USD/BRL na Twelve Data");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaExchangeRate(BigDecimal rate) {
    }
}
