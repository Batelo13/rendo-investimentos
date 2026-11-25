package com.curso.gestaoinvestimentos.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Configuracao do JWT falha cedo (na subida) em vez de emitir token fraco. */
class JwtPropertiesTest {

    private static final String SEGREDO_32 = "a".repeat(32);

    @Test
    void aceitaSegredoDe32BytesValidadeEEmissor() {
        JwtProperties props = new JwtProperties(SEGREDO_32, Duration.ofMinutes(60), "rendo");
        assertThat(props.expiration()).hasMinutes(60);
    }

    @Test
    void recusaSegredoDe31Bytes() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtProperties("a".repeat(31), Duration.ofMinutes(60), "rendo"))
                .withMessageContaining("32 bytes");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void recusaSegredoAusente(String segredo) {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtProperties(segredo, Duration.ofMinutes(60), "rendo"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void recusaValidadeZeroOuNegativa(long segundos) {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtProperties(SEGREDO_32, Duration.ofSeconds(segundos), "rendo"))
                .withMessageContaining("expiration");
    }

    @Test
    void recusaValidadeAusente() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtProperties(SEGREDO_32, null, "rendo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void recusaEmissorEmBranco(String emissor) {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtProperties(SEGREDO_32, Duration.ofMinutes(60), emissor))
                .withMessageContaining("issuer");
    }
}
