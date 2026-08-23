package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.integration.IbovespaClient;
import com.curso.gestaoinvestimentos.integration.IndiceMercado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mercado")
public class MercadoController {

    private final IbovespaClient ibovespaClient;

    public MercadoController(IbovespaClient ibovespaClient) {
        this.ibovespaClient = ibovespaClient;
    }

    @GetMapping("/ibovespa")
    public IndiceMercado ibovespa() {
        return ibovespaClient.buscar();
    }
}
