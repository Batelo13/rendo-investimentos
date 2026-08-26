package com.curso.gestaoinvestimentos.security.oauth2;

import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Login social nao cria conta nova: so autentica quem ja tem cadastro no Rendo
 * com o mesmo email. Motivo: o cadastro exige CPF (unico, obrigatorio) e nenhum
 * provedor OAuth2/OIDC devolve esse dado -- criar a conta automaticamente
 * exigiria tornar CPF opcional ou um passo extra de "completar cadastro", o que
 * e uma mudanca de regra de negocio fora do escopo deste redesign visual (fica
 * documentado no relatorio como decisao deliberada, nao esquecimento).
 */
@Service
public class RendoOidcUserService extends OidcUserService {

    private final UsuarioRepository usuarioRepository;

    public RendoOidcUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_indisponivel"),
                    "Nao foi possivel obter o email da conta " + userRequest.getClientRegistration().getClientName());
        }

        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(() ->
                new ContaOAuthNaoEncontradaException(oidcUser.getFullName(), email));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("conta_bloqueada"), "Conta bloqueada.");
        }

        // O provedor OAuth2/OIDC ja comprova a posse do email -- uma autenticacao
        // social bem-sucedida "cura" uma verificacao pendente, sem bloquear o
        // login (ao contrario do formLogin, que exige o codigo por email).
        if (!Boolean.TRUE.equals(usuario.getEmailVerified())) {
            usuario.setEmailVerified(true);
            usuarioRepository.save(usuario);
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
        // nameAttributeKey = "email": Principal.getName() (usado em toda a API
        // via Principal.getName(), ex. CarteiraController) passa a devolver o
        // mesmo email usado no login por formulario, sem precisar de nenhuma
        // mudanca nos controllers/services existentes.
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}
