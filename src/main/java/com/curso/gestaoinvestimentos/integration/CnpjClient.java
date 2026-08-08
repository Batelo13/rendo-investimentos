package com.curso.gestaoinvestimentos.integration;

public interface CnpjClient {

    DadosCnpjResponse buscar(String cnpj);
}
