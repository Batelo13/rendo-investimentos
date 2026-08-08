package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class BrapiCotacaoProvider implements CotacaoProvider {

    private final RestClient restClient;
    private final String token;

    public BrapiCotacaoProvider(@Value("${brapi.api.token:}") String token) {
        this.token = token;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://brapi.dev/api")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean suporta(Mercado mercado) {
        return mercado == Mercado.BRASIL;
    }

    @Override
    public DadosCotacaoResponse buscarCotacao(String ticker) {
        try {
            RespostaBrapi resposta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/{ticker}")
                            .queryParamIfPresent("token", Optional.ofNullable(token).filter(t -> !t.isBlank()))
                            .build(ticker))
                    .retrieve()
                    .body(RespostaBrapi.class);

            if (resposta == null || resposta.results() == null || resposta.results().isEmpty()) {
                throw new RecursoNaoEncontradoException("Ticker nao encontrado na brapi: " + ticker);
            }

            Cotacao cotacao = resposta.results().get(0);
            String nome = cotacao.longName() != null ? cotacao.longName() : cotacao.shortName();

            return new DadosCotacaoResponse(
                    cotacao.symbol(),
                    nome,
                    cotacao.currency(),
                    cotacao.regularMarketPrice(),
                    OffsetDateTime.parse(cotacao.regularMarketTime()).toLocalDateTime()
            );
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatusCode.valueOf(401)) {
                throw new ServicoExternoIndisponivelException(
                        "brapi exigiu token de autenticacao para este ticker. Configure BRAPI_API_TOKEN.");
            }
            throw new RecursoNaoEncontradoException("Ticker nao encontrado na brapi: " + ticker);
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar a cotacao na brapi");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaBrapi(List<Cotacao> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Cotacao(
            String symbol,
            String shortName,
            String longName,
            String currency,
            BigDecimal regularMarketPrice,
            String regularMarketTime
    ) {
    }
}
