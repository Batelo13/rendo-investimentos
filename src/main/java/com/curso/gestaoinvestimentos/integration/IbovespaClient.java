package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * So o indice IBOVESPA, pra alimentar o widget "IBOV" da Visao Geral -- fluxo
 * separado do CotacaoProvider (que e o contrato real de compra/venda de
 * acoes) porque brapi devolve regularMarketChangePercent aqui, campo que
 * DadosCotacaoResponse nao tem e que o restante do app nao precisa.
 */
@Component
public class IbovespaClient {

    private final RestClient restClient;
    private final String token;

    public IbovespaClient(@Value("${brapi.api.token:}") String token) {
        this.token = token;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://brapi.dev/api")
                .requestFactory(requestFactory)
                .build();
    }

    @CircuitBreaker(name = "ibovespa", fallbackMethod = "buscarFallback")
    public IndiceMercado buscar() {
        try {
            RespostaBrapi resposta = RetryExterno.tentar(3, 300, () -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/^BVSP")
                            .queryParamIfPresent("token", Optional.ofNullable(token).filter(t -> !t.isBlank()))
                            .build())
                    .retrieve()
                    .body(RespostaBrapi.class));

            if (resposta == null || resposta.results() == null || resposta.results().isEmpty()) {
                throw new ServicoExternoIndisponivelException("Indice IBOVESPA indisponivel no momento");
            }

            Cotacao c = resposta.results().get(0);
            return new IndiceMercado("IBOV", c.regularMarketPrice(), c.regularMarketChangePercent(),
                    OffsetDateTime.parse(c.regularMarketTime()).toLocalDateTime());
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar o IBOVESPA");
        }
    }

    private IndiceMercado buscarFallback(Throwable t) {
        throw new ServicoExternoIndisponivelException("IBOVESPA indisponivel no momento (circuit breaker aberto)");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaBrapi(List<Cotacao> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Cotacao(
            BigDecimal regularMarketPrice,
            BigDecimal regularMarketChangePercent,
            String regularMarketTime
    ) {
    }
}
