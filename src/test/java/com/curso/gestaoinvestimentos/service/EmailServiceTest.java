package com.curso.gestaoinvestimentos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Modulo 11 (dados sensiveis em log): codigo de verificacao e email. */
@ExtendWith(OutputCaptureExtension.class)
class EmailServiceTest {

    private EmailService servico(boolean logCodigo) {
        return new EmailService(mock(JavaMailSender.class), mock(TemplateEngine.class),
                false, "no-reply@rendo.app", logCodigo);
    }

    @Test
    @DisplayName("Scenario: Envio desligado sem registro de codigo habilitado")
    void naoLogaCodigoNemEmailCompleto(CapturedOutput saida) {
        servico(false).enviarCodigoVerificacao("pessoa.real@example.com", "Pessoa", "482913");

        assertThat(saida.getOut()).doesNotContain("482913").doesNotContain("pessoa.real@example.com");
        assertThat(saida.getOut()).contains("p***@example.com");
    }

    @Test
    void logaCodigoSomenteQuandoHabilitadoExplicitamente(CapturedOutput saida) {
        servico(true).enviarCodigoVerificacao("dev@example.com", "Dev", "105377");

        assertThat(saida.getOut()).contains("105377").doesNotContain("dev@example.com");
    }

    @Test
    void mascaraEmail() {
        assertThat(EmailService.mascararEmail("ana@exemplo.com.br")).isEqualTo("a***@exemplo.com.br");
        assertThat(EmailService.mascararEmail("sem-arroba")).isEqualTo("***");
        assertThat(EmailService.mascararEmail(null)).isEqualTo("***");
    }
}
