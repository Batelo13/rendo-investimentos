package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.model.Mercado;

public interface CotacaoProvider {

    boolean suporta(Mercado mercado);

    DadosCotacaoResponse buscarCotacao(String ticker);
}
