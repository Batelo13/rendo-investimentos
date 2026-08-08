package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiCnpjClient implements CnpjClient {

    private final RestClient restClient;

    public BrasilApiCnpjClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://brasilapi.com.br/api/cnpj/v1")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public DadosCnpjResponse buscar(String cnpj) {
        try {
            RespostaBrasilApi resposta = restClient.get()
                    .uri("/{cnpj}", cnpj)
                    .retrieve()
                    .body(RespostaBrasilApi.class);

            if (resposta == null) {
                throw new RecursoNaoEncontradoException("CNPJ nao encontrado: " + cnpj);
            }

            return new DadosCnpjResponse(
                    resposta.cnpj(),
                    resposta.razao_social(),
                    resposta.nome_fantasia(),
                    resposta.logradouro(),
                    resposta.numero(),
                    resposta.complemento(),
                    resposta.bairro(),
                    resposta.cep(),
                    resposta.ddd_telefone_1(),
                    resposta.descricao_situacao_cadastral()
            );
        } catch (HttpClientErrorException ex) {
            throw new RecursoNaoEncontradoException("CNPJ nao encontrado ou invalido: " + cnpj);
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar o CNPJ na BrasilAPI");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaBrasilApi(
            String cnpj,
            String razao_social,
            String nome_fantasia,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cep,
            String descricao_situacao_cadastral,
            String ddd_telefone_1
    ) {
    }
}
