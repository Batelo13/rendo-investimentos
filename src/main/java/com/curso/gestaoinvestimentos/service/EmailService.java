package com.curso.gestaoinvestimentos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean mailEnabled;
    private final String remetente;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                         @Value("${app.mail.enabled:true}") boolean mailEnabled,
                         @Value("${app.mail.from:no-reply@rendo.app}") String remetente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailEnabled = mailEnabled;
        this.remetente = remetente;
    }

    public void enviarCodigoVerificacao(String destinatario, String nome, String codigo) {
        if (!mailEnabled) {
            log.info("MAIL_ENABLED=false -- email de verificacao para {} nao foi enviado (envio real desligado).", destinatario);
            return;
        }

        Context context = new Context();
        context.setVariable("nome", nome);
        context.setVariable("codigo", codigo);
        String corpo = templateEngine.process("email/verificacao-email", context);

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("Confirme seu e-mail");
            helper.setText(corpo, true);
            mailSender.send(mensagem);
        } catch (Exception e) {
            log.error("Falha ao enviar email de verificacao para {}", destinatario, e);
            throw new IllegalStateException("Nao foi possivel enviar o email de verificacao.", e);
        }
    }
}
