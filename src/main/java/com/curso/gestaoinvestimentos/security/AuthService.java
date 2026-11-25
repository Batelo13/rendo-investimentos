package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.dto.LoginRequestDTO;
import com.curso.gestaoinvestimentos.dto.TokenResponseDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Aula 13: login por JWT. Mesmas regras do login por formulario -- mesmo
 * UserDetailsService (conta bloqueada, email nao verificado), BCrypt e limite
 * de tentativas. O token so leva email e papeis (o payload e legivel por quem
 * tiver o token: assinado, nao cifrado).
 */
@Service
public class AuthService {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final LimiteTentativasLogin limiteTentativas;

    public AuthService(AuthenticationConfiguration authenticationConfiguration, JwtEncoder jwtEncoder,
                       JwtProperties props, LimiteTentativasLogin limiteTentativas) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.limiteTentativas = limiteTentativas;
    }

    public TokenResponseDTO login(LoginRequestDTO dto, String ip) {
        if (limiteTentativas.bloqueado(ip, dto.email())) {
            throw new MuitasTentativasException();
        }
        Authentication autenticacao;
        try {
            autenticacao = gerenciador().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.senha()));
        } catch (AuthenticationException falha) {
            limiteTentativas.registrarFalha(ip, dto.email());
            // Mensagem unica: nao revela se o email existe, se a conta esta
            // bloqueada ou se falta verificar o email.
            throw new BadCredentialsException("Credenciais invalidas ou conta indisponivel");
        }
        limiteTentativas.limpar(ip, dto.email());

        Instant agora = Instant.now();
        // So os papeis: o Spring Security 7 tambem poe autoridades FACTOR_* (fator usado no login).
        List<String> papeis = autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(agora)
                .expiresAt(agora.plus(props.expiration()))
                .subject(autenticacao.getName())
                .claim(JwtConfig.CLAIM_PAPEIS, papeis)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenResponseDTO(token, "Bearer", props.expiration().toSeconds());
    }

    private AuthenticationManager gerenciador() {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            throw new IllegalStateException("AuthenticationManager indisponivel", e);
        }
    }
}
