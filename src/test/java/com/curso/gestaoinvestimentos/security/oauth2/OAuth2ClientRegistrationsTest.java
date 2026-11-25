package com.curso.gestaoinvestimentos.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Login social: cada provedor so entra com o par client-id + secret completo. */
class OAuth2ClientRegistrationsTest {

    private OAuth2ClientRegistrations config(String gId, String gSecret, String mId, String mSecret) {
        OAuth2ClientRegistrations config = new OAuth2ClientRegistrations();
        ReflectionTestUtils.setField(config, "googleClientId", gId);
        ReflectionTestUtils.setField(config, "googleClientSecret", gSecret);
        ReflectionTestUtils.setField(config, "microsoftClientId", mId);
        ReflectionTestUtils.setField(config, "microsoftClientSecret", mSecret);
        ReflectionTestUtils.setField(config, "microsoftTenantId", "meu-tenant");
        return config;
    }

    private InMemoryClientRegistrationRepository repo(OAuth2ClientRegistrations config) {
        return (InMemoryClientRegistrationRepository) config.clientRegistrationRepository();
    }

    @Test
    void soGoogleConfiguradoRegistraSoGoogle() {
        var repo = repo(config("g-id", "g-secret", "", ""));

        ClientRegistration google = repo.findByRegistrationId("google");
        assertThat(google.getClientId()).isEqualTo("g-id");
        assertThat(google.getScopes()).contains("openid", "email");
        assertThat(repo.findByRegistrationId("microsoft")).isNull();
    }

    @Test
    void microsoftUsaOTenantConfiguradoNosEndpoints() {
        var repo = repo(config("", "", "m-id", "m-secret"));

        ClientRegistration microsoft = repo.findByRegistrationId("microsoft");
        assertThat(microsoft.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://login.microsoftonline.com/meu-tenant/oauth2/v2.0/authorize");
        assertThat(repo.findByRegistrationId("google")).isNull();
    }

    @Test
    void osDoisConfiguradosRegistramOsDois() {
        var repo = repo(config("g-id", "g-secret", "m-id", "m-secret"));

        assertThat(repo).extracting(ClientRegistration::getRegistrationId).containsExactlyInAnyOrder("google", "microsoft");
    }

    @Test
    void idSemSecretFalhaNaSubidaComMensagemClara() {
        assertThatIllegalStateException()
                .isThrownBy(() -> config("g-id", "", "", "").clientRegistrationRepository())
                .withMessageContaining("parcialmente configurado");
    }
}
