package com.curso.gestaoinvestimentos.integration;

public record DadosCnpjResponse(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cep,
        String telefone,
        String situacaoCadastral,
        String cnaePrincipal
) {
}
