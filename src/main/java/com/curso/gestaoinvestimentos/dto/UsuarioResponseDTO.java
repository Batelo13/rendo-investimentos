package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Role;

import java.time.LocalDate;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cpf,
        Role role,
        Boolean ativo,
        LocalDate dataCadastro
) {
}
