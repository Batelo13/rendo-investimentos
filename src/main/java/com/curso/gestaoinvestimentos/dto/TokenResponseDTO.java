package com.curso.gestaoinvestimentos.dto;

/** expiresIn em segundos. */
public record TokenResponseDTO(String token, String tipo, long expiresIn) {
}
