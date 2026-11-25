package com.curso.gestaoinvestimentos.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Aula 13: configuracao do JWT (app.jwt.*, vinda do .env). Valida na subida:
 * HS256 exige chave de pelo menos 256 bits, e um token sem validade ou sem
 * emissor nao deve nem ser emitido.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) deve ter pelo menos 32 bytes");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("app.jwt.expiration deve ser positiva");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("app.jwt.issuer deve ser informado");
        }
    }
}
