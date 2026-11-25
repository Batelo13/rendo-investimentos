package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.validation.TamanhoMaximoBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, message = UsuarioRequestDTO.MSG_SENHA_TAMANHO)
        @Pattern(regexp = UsuarioRequestDTO.SENHA_FORTE, message = UsuarioRequestDTO.MSG_SENHA_FORTE)
        @TamanhoMaximoBytes(value = 72, message = "Senha deve ter no maximo 72 bytes")
        String senha
) {
}
