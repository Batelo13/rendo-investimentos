package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Role;
import jakarta.validation.constraints.NotNull;

public record AlterarPapelRequestDTO(@NotNull(message = "Papel e obrigatorio") Role role) {
}
