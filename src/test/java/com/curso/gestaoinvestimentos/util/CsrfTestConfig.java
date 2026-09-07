package com.curso.gestaoinvestimentos.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Aplica um token CSRF valido por padrao a toda requisicao do MockMvc, ja que
 * o filtro de seguranca de verdade roda nos testes ({@code @AutoConfigureMockMvc})
 * e agora exige o token em POST/PATCH/DELETE. Evita repetir {@code .with(csrf())}
 * em cada chamada -- quem precisa testar a ausencia do token faz isso explicitamente
 * com {@code .with(csrf().useInvalidToken())} ou removendo o default request.
 */
@TestConfiguration
public class CsrfTestConfig {

    @Bean
    public MockMvcBuilderCustomizer csrfPorPadrao() {
        return builder -> builder.defaultRequest(get("/").with(csrf()));
    }
}
