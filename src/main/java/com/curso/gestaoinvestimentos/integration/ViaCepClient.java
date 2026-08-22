package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepClient implements CepClient {

    private final RestClient restClient;

    public ViaCepClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br/ws")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Cacheable("cep")
    @CircuitBreaker(name = "cep", fallbackMethod = "buscarFallback")
    public DadosCepResponse buscar(String cep) {
        try {
            RespostaViaCep resposta = RetryExterno.tentar(3, 300, () -> restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(RespostaViaCep.class));

            if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
                throw new RecursoNaoEncontradoException("CEP nao encontrado: " + cep);
            }

            return new DadosCepResponse(
                    resposta.cep(),
                    resposta.logradouro(),
                    resposta.bairro(),
                    resposta.localidade(),
                    resposta.uf()
            );
        } catch (RecursoNaoEncontradoException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar o CEP no ViaCEP");
        }
    }

    private DadosCepResponse buscarFallback(String cep, Throwable t) {
        throw new ServicoExternoIndisponivelException("ViaCEP indisponivel no momento (circuit breaker aberto)");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaViaCep(
            String cep,
            String logradouro,
            String bairro,
            String localidade,
            String uf,
            Boolean erro
    ) {
    }
}
