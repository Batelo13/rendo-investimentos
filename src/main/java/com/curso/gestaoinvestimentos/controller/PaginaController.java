package com.curso.gestaoinvestimentos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Rotas MVC (retornam nome de view Thymeleaf, nao JSON) -- diferente dos
 * outros controllers do projeto, que sao todos @RestController. Cresce
 * conforme mais paginas server-side forem construidas.
 */
@Controller
public class PaginaController {

    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;

    public PaginaController(Optional<ClientRegistrationRepository> clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/login")
    public String login(Model model) {
        // So aparecem na tela os provedores de login social REALMENTE
        // configurados (ver OAuth2ClientRegistrations) -- sem credenciais, o
        // set fica vazio e a secao inteira some do template.
        Set<String> provedoresSociais = new LinkedHashSet<>();
        clientRegistrationRepository.ifPresent(repo -> {
            if (repo instanceof Iterable<?> registros) {
                for (Object registro : registros) {
                    if (registro instanceof ClientRegistration clientRegistration) {
                        provedoresSociais.add(clientRegistration.getRegistrationId());
                    }
                }
            }
        });
        model.addAttribute("provedoresSociais", provedoresSociais);
        return "login";
    }

    // A area de administracao so e renderizada para ADMIN (USER nem recebe o
    // HTML); os dados dela vem de endpoints que tambem exigem ADMIN.
    @GetMapping("/dashboard")
    public String dashboard(Authentication autenticacao, Model model) {
        model.addAttribute("admin", autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        return "dashboard";
    }
}
