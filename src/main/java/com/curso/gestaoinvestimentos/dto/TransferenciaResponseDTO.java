package com.curso.gestaoinvestimentos.dto;

public record TransferenciaResponseDTO(
        OperacaoResponseDTO saida,
        OperacaoResponseDTO entrada
) {
}
