package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CorretoraRequestDTO(

        @NotBlank(message = "CNPJ e obrigatorio")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 digitos numericos, sem pontuacao")
        String cnpj,

        @NotBlank(message = "Razao social e obrigatoria")
        String razaoSocial,

        String nomeFantasia,

        @Email(message = "Email invalido")
        String email,

        String telefone,

        @NotBlank(message = "CEP e obrigatorio")
        @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 digitos numericos, sem pontuacao")
        String cep,

        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {
}
