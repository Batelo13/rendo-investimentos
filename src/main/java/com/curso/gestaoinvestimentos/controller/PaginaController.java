package com.curso.gestaoinvestimentos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotas MVC (retornam nome de view Thymeleaf, nao JSON) -- diferente dos
 * outros controllers do projeto, que sao todos @RestController. Cresce
 * conforme mais paginas server-side forem construidas.
 */
@Controller
public class PaginaController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
