package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.validation.CPF;
import com.curso.gestaoinvestimentos.validation.TamanhoMaximoBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Aula 14: conta criada pelo ADMIN -- mesmas regras do cadastro + papel. */
public record UsuarioAdminRequestDTO(

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
        @Size(min = 8, message = UsuarioRequestDTO.MSG_SENHA_TAMANHO)
        @Pattern(regexp = UsuarioRequestDTO.SENHA_FORTE, message = UsuarioRequestDTO.MSG_SENHA_FORTE)
        @TamanhoMaximoBytes(value = 72, message = "Senha deve ter no maximo 72 bytes")
        String senha,

        @NotNull(message = "Papel e obrigatorio")
        Role role
) {
}
