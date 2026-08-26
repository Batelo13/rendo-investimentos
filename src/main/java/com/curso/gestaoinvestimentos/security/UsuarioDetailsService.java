package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        // So checa email nao verificado se a conta estiver ativa -- bloqueio por
        // admin (ativo=false) continua tendo prioridade e usa o DisabledException
        // padrao do framework (via .disabled(true) abaixo), comportamento ja
        // testado. "null" (contas anteriores a este campo, sem backfill de
        // ddl-auto=update) e tratado como verificado, nunca bloqueia login.
        if (Boolean.TRUE.equals(usuario.getAtivo()) && Boolean.FALSE.equals(usuario.getEmailVerified())) {
            throw new EmailNaoVerificadoException("Confirme seu email antes de entrar.");
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(usuario.getRole().name())
                .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                .build();
    }
}
