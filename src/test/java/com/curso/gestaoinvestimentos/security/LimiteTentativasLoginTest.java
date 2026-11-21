package com.curso.gestaoinvestimentos.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteTentativasLoginTest {

    private static class RelogioAjustavel extends Clock {
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");
        public ZoneOffset getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return agora; }
    }

    @Test
    @DisplayName("Scenario: Bloqueio expira depois da janela")
    void bloqueioExpiraDepoisDaJanela() {
        RelogioAjustavel relogio = new RelogioAjustavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("1.1.1.1", "a@x.com");
        }
        assertThat(limite.bloqueado("1.1.1.1", "a@x.com")).isTrue();

        relogio.agora = relogio.agora.plus(LimiteTentativasLogin.JANELA).plusSeconds(1);
        assertThat(limite.bloqueado("1.1.1.1", "a@x.com")).isFalse();
    }

    @Test
    void quatroFalhasAindaNaoBloqueiam() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(Clock.systemUTC());
        for (int i = 0; i < 4; i++) {
            limite.registrarFalha("1.1.1.1", "a@x.com");
        }
        assertThat(limite.bloqueado("1.1.1.1", "a@x.com")).isFalse();
        assertThat(LimiteTentativasLogin.JANELA).isEqualTo(Duration.ofMinutes(15));
    }
}
