package com.curso.gestaoinvestimentos.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * A Apple exige response_mode=form_post sempre que o scope pede algo alem de
 * "openid" (aqui pedimos "name"/"email" tambem) -- sem isso ela rejeita a
 * autorizacao. Nenhum outro provedor precisa desse parametro extra.
 */
public class AppleAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String BASE_URI = "/oauth2/authorization";
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public AppleAuthorizationRequestResolver(ClientRegistrationRepository repository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repository, BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return comFormPostSeApple(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return comFormPostSeApple(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest comFormPostSeApple(OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null || !authRequest.getAuthorizationUri().contains("appleid.apple.com")) {
            return authRequest;
        }
        Map<String, Object> extras = new HashMap<>(authRequest.getAdditionalParameters());
        extras.put("response_mode", "form_post");
        return OAuth2AuthorizationRequest.from(authRequest).additionalParameters(extras).build();
    }
}
