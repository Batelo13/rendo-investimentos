package com.curso.gestaoinvestimentos.integration;

public record DadosCepResponse(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String uf
) {
}
