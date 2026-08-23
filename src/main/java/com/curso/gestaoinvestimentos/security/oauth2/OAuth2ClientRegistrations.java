package com.curso.gestaoinvestimentos.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Login social (Google/Microsoft) e "estrutura pronta, nao funcionalidade
 * falsa": cada provedor so entra na lista se AMBOS client-id/secret estiverem
 * configurados via variavel de ambiente. Sem nenhum configurado, este bean
 * retorna null (nenhum registro) e o SecurityConfig simplesmente nao ativa
 * oauth2Login() -- a aplicacao sobe normal, e os botoes na tela de login
 * apontam pra rotas reais que so passam a funcionar quando as credenciais
 * forem cadastradas (ver README/env vars: GOOGLE_CLIENT_ID/SECRET,
 * MICROSOFT_CLIENT_ID/SECRET).
 *
 * Os dois sao tratados como OIDC (scope "openid"): mais robusto que OAuth2
 * puro (claims assinadas, verificadas via JWKS).
 */
@Configuration
public class OAuth2ClientRegistrations {

    @Value("${oauth2.google.client-id:}")
    private String googleClientId;
    @Value("${oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth2.microsoft.client-id:}")
    private String microsoftClientId;
    @Value("${oauth2.microsoft.client-secret:}")
    private String microsoftClientSecret;
    @Value("${oauth2.microsoft.tenant-id:common}")
    private String microsoftTenantId;

    // @ConditionalOnExpression (nao Optional<> no ponto de injecao): o proprio
    // spring-security-oauth2-client exige, so por estar no classpath com
    // @EnableWebSecurity, que ALGUM bean ClientRegistrationRepository exista --
    // um bean que existe mas resolve pra null nao basta (o restante da config
    // trata "bean ausente" e "bean nulo" de formas diferentes). Por isso a
    // definicao do bean so e registrada quando ha client-id configurado; sem
    // nenhum, nao existe candidato nenhum, exatamente como se a dependencia nao
    // estivesse no projeto.
    @Bean
    @ConditionalOnExpression("!('${oauth2.google.client-id:}' + '${oauth2.microsoft.client-id:}').isBlank()")
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registros = new ArrayList<>();

        if (configurado(googleClientId, googleClientSecret)) {
            registros.add(google());
        }
        if (configurado(microsoftClientId, microsoftClientSecret)) {
            registros.add(microsoft());
        }

        if (registros.isEmpty()) {
            throw new IllegalStateException(
                    "Login social parcialmente configurado: um client-id foi definido sem o client-secret correspondente (ou vice-versa). Configure o par completo no .env ou remova ambos.");
        }
        return new InMemoryClientRegistrationRepository(registros);
    }

    private boolean configurado(String clientId, String clientSecret) {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    private ClientRegistration google() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userNameAttributeName("email")
                .clientName("Google")
                .build();
    }

    // Sem CommonOAuth2Provider pronto pra Microsoft: endpoints do tenant
    // "common" (contas pessoais + corporativas/escolares) do v2.0 do Azure AD.
    private ClientRegistration microsoft() {
        String base = "https://login.microsoftonline.com/" + microsoftTenantId;
        return ClientRegistration.withRegistrationId("microsoft")
                .clientId(microsoftClientId)
                .clientSecret(microsoftClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(base + "/oauth2/v2.0/authorize")
                .tokenUri(base + "/oauth2/v2.0/token")
                .jwkSetUri(base + "/discovery/v2.0/keys")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .userNameAttributeName("email")
                .clientName("Microsoft")
                .build();
    }
}
