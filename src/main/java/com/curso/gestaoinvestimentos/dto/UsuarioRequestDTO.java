package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.validation.CPF;
import com.curso.gestaoinvestimentos.validation.TamanhoMaximoBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 digitos numericos, sem pontuacao")
        @CPF(message = "CPF invalido")
        String cpf,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
        @TamanhoMaximoBytes(value = 72, message = "Senha deve ter no maximo 72 bytes")
        String senha
) {
}
