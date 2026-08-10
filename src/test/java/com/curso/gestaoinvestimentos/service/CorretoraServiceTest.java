package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.CorretoraRequestDTO;
import com.curso.gestaoinvestimentos.dto.CorretoraResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.integration.CepClient;
import com.curso.gestaoinvestimentos.integration.CnpjClient;
import com.curso.gestaoinvestimentos.integration.DadosCepResponse;
import com.curso.gestaoinvestimentos.integration.DadosCnpjResponse;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorretoraServiceTest {

    @Mock
    private CorretoraRepository repository;
    @Mock
    private CnpjClient cnpjClient;
    @Mock
    private CepClient cepClient;

    @InjectMocks
    private CorretoraService service;

    private static final String CNPJ = "12345678000199";
    private static final CorretoraRequestDTO REQUEST = new CorretoraRequestDTO(CNPJ);

    @Test
    void deveValidarNaCvmQuandoCnaeECorretoraEAtiva() {
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.empty());
        when(cnpjClient.buscar(CNPJ)).thenReturn(new DadosCnpjResponse(
                CNPJ, "Corretora Exemplo Ltda", "Corretora Exemplo", "Rua A", "1", null,
                "Centro", "01000-000", "1122223333", "ATIVA", "6612602"));
        when(cepClient.buscar("01000-000")).thenReturn(
                new DadosCepResponse("01000-000", "Rua A", "Centro", "Sao Paulo", "SP"));
        when(repository.save(any(Corretora.class))).thenAnswer(inv -> inv.getArgument(0));

        CorretoraResponseDTO resultado = service.criar(REQUEST);

        assertEquals(Boolean.TRUE, resultado.validadaNaCvm());
    }

    @Test
    void deveRejeitarQuandoSituacaoCadastralNaoAtiva() {
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.empty());
        when(cnpjClient.buscar(CNPJ)).thenReturn(new DadosCnpjResponse(
                CNPJ, "Corretora Exemplo Ltda", "Corretora Exemplo", "Rua A", "1", null,
                "Centro", "01000-000", "1122223333", "BAIXADA", "6612602"));

        assertThrows(RegraDeNegocioException.class, () -> service.criar(REQUEST));
    }

    @Test
    void deveRejeitarQuandoCnaeNaoECorretora() {
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.empty());
        when(cnpjClient.buscar(CNPJ)).thenReturn(new DadosCnpjResponse(
                CNPJ, "Supermercado Exemplo Ltda", "Supermercado Exemplo", "Rua A", "1", null,
                "Centro", "01000-000", "1122223333", "ATIVA", "4711301"));

        assertThrows(RegraDeNegocioException.class, () -> service.criar(REQUEST));
    }

    @Test
    void deveRejeitarQuandoJaExisteCorretoraComMesmoCnpj() {
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.of(new Corretora()));

        assertThrows(RecursoDuplicadoException.class, () -> service.criar(REQUEST));
    }
}
