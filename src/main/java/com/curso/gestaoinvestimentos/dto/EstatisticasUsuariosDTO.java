package com.curso.gestaoinvestimentos.dto;

/** Painel do administrador: contagens de contas, sem dado pessoal. */
public record EstatisticasUsuariosDTO(
        long total,
        long ativas,
        long bloqueadas,
        long emailNaoVerificado,
        long administradores,
        long novasUltimos30Dias
) {
}
