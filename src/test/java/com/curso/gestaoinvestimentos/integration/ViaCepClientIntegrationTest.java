package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Modulo 11 (tratamento seguro de erros): o fallback do circuit breaker rodava
 * para QUALQUER excecao -- "CEP inexistente" virava 503 "circuit breaker
 * aberto". Usa o bean real (com o proxy do Resilience4j) apontado para um
 * servidor HTTP simulado.
 */
@SpringBootTest
class ViaCepClientIntegrationTest {

    @Autowired
    private CepClient cepClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer servidor;

    @BeforeEach
    void apontarParaServidorSimulado() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test/ws");
        servidor = MockRestServiceServer.bindTo(builder).build();
        ViaCepClient alvo = AopTestUtils.getUltimateTargetObject(cepClient);
        ReflectionTestUtils.setField(alvo, "restClient", builder.build());
    }

    @AfterEach
    void fecharCircuito() {
        circuitBreakerRegistry.circuitBreaker("cep").reset();
    }

    @Test
    @DisplayName("Scenario: CEP inexistente com circuito fechado")
    void cepInexistenteChegaComoNaoEncontrado() {
        servidor.expect(requestTo("https://viacep.test/ws/99999001/json/"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cepClient.buscar("99999001"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("CEP nao encontrado");
    }

    @Test
    void circuitoAbertoContinuaRespondendoIndisponivel() {
        circuitBreakerRegistry.circuitBreaker("cep").transitionToOpenState();

        assertThatThrownBy(() -> cepClient.buscar("99999002"))
                .isInstanceOf(ServicoExternoIndisponivelException.class)
                .hasMessageContaining("circuit breaker aberto");
        assertThatThrownBy(() -> circuitBreakerRegistry.circuitBreaker("cep").acquirePermission())
                .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
        org.assertj.core.api.Assertions.assertThat(circuitBreakerRegistry.circuitBreaker("cep").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }
}
