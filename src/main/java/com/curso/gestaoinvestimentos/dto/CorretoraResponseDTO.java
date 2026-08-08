package com.curso.gestaoinvestimentos.dto;

import java.time.LocalDate;

public record CorretoraResponseDTO(
        Long id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String situacaoCadastral,
        Boolean validadaNaCvm,
        LocalDate dataCadastro
) {
}
