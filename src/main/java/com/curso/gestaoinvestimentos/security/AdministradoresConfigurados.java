package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Papel ADMIN vem so da configuracao do servidor (ADMIN_EMAILS, no .env) --
 * nenhum endpoint altera papel, e o email do administrador nao fica no codigo.
 * Aplicado no cadastro e, na inicializacao, a usuarios que ja existiam.
 */
@Component
public class AdministradoresConfigurados implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradoresConfigurados.class);

    private final Set<String> emails;
    private final UsuarioRepository usuarioRepository;

    public AdministradoresConfigurados(@Value("${app.admin.emails:}") String emails, UsuarioRepository usuarioRepository) {
        this.emails = Arrays.stream(emails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.usuarioRepository = usuarioRepository;
    }

    public boolean ehAdministrador(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Usuario> promovidos = usuarioRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN && ehAdministrador(u.getEmail()))
                .toList();
        promovidos.forEach(u -> u.setRole(Role.ADMIN));
        usuarioRepository.saveAll(promovidos);
        if (!promovidos.isEmpty()) {
            log.info("ADMIN_EMAILS: {} usuario(s) promovido(s) a ADMIN", promovidos.size());
        }
    }
}
