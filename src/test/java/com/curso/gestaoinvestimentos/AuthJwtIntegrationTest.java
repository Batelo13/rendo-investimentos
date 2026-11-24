package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import com.curso.gestaoinvestimentos.util.CsrfTestConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aulas 13 e 14: login JWT, rotas com Bearer e controle de acesso por papel.
 * MockMvc proprio, SEM o csrf() padrao do CsrfTestConfig: um cliente de API
 * real nao tem token CSRF, e e isso que estes testes precisam provar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
class AuthJwtIntegrationTest {

    private static final String SENHA = "Senha1234";

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CarteiraRepository carteiraRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc api;

    @BeforeEach
    void montarCliente() {
        api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Usuario cadastrar(String email, Role role, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario JWT");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setRole(role);
        usuario.setAtivo(ativo);
        usuario.setEmailVerified(true);
        usuario.setDataCadastro(LocalDate.now());
        Usuario salvo = usuarioRepository.save(usuario);
        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(new BigDecimal("100000.00"));
        carteiraRepository.save(carteira);
        return salvo;
    }

    private ResultActions login(String ip, String email, String senha) throws Exception {
        return api.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
                .with(req -> { req.setRemoteAddr(ip); return req; }));
    }

    private String token(String email) throws Exception {
        MvcResult resultado = login("10.1.0.1", email, SENHA).andExpect(status().isOk()).andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("Scenario: Credenciais validas")
    void loginValidoDevolveTokenBearerSemCsrf() throws Exception {
        cadastrar("jwt.valido@example.com", Role.USER, true);

        MvcResult resultado = login("10.1.0.2", "jwt.valido@example.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andReturn();

        // Sem sessao: o fluxo JWT e stateless.
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("Scenario: Senha errada")
    void senhaErradaDevolve401SemToken() throws Exception {
        cadastrar("jwt.errada@example.com", Role.USER, true);

        login("10.1.0.3", "jwt.errada@example.com", "Errada123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void contaBloqueadaDevolve401() throws Exception {
        cadastrar("jwt.bloqueada@example.com", Role.USER, false);

        login("10.1.0.4", "jwt.bloqueada@example.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario: Rota protegida sem token")
    void rotaProtegidaSemTokenDevolve401() throws Exception {
        api.perform(get("/carteiras/me/saldo")).andExpect(status().isUnauthorized());
        api.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario: Token valido")
    void tokenValidoAcessaRotaEMe() throws Exception {
        cadastrar("jwt.me@example.com", Role.USER, true);
        String token = token("jwt.me@example.com");

        api.perform(get("/carteiras/me/saldo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        api.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwt.me@example.com"))
                .andExpect(jsonPath("$.papeis", contains("ROLE_USER")));
    }

    @Test
    @DisplayName("Scenario: Token adulterado ou expirado")
    void tokenAdulteradoDevolve401() throws Exception {
        cadastrar("jwt.adulterado@example.com", Role.USER, true);
        String token = token("jwt.adulterado@example.com");
        char ultimo = token.charAt(token.length() - 1);
        String adulterado = token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        api.perform(get("/carteiras/me/saldo").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario: Token adulterado ou expirado")
    void tokenExpiradoDevolve401() throws Exception {
        cadastrar("jwt.expirado@example.com", Role.USER, true);
        Instant passado = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("rendo").subject("jwt.expirado@example.com")
                .issuedAt(passado).expiresAt(passado.plusSeconds(60))
                .claim("authorities", List.of("ROLE_USER")).build();
        String expirado = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();

        api.perform(get("/carteiras/me/saldo").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario: USER com token tenta area de admin")
    void userComTokenRecebe403NaAreaAdmin() throws Exception {
        Usuario user = cadastrar("jwt.user@example.com", Role.USER, true);
        String token = token("jwt.user@example.com");

        api.perform(get("/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        api.perform(get("/usuarios/estatisticas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        api.perform(patch("/usuarios/{id}/bloquear", user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        api.perform(get("/carteiras/{id}", user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario: ADMIN com token")
    void adminComTokenGerenciaUsuariosSemCsrf() throws Exception {
        cadastrar("jwt.admin@example.com", Role.ADMIN, true);
        Usuario alvo = cadastrar("jwt.alvo@example.com", Role.USER, true);
        String token = token("jwt.admin@example.com");

        api.perform(get("/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        // Escrita com Bearer nao exige CSRF (o token nao vai em cookie).
        api.perform(patch("/usuarios/{id}/bloquear", alvo.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        // E a conta bloqueada nao consegue mais um token novo.
        login("10.1.0.5", "jwt.alvo@example.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario: Muitas tentativas")
    void muitasTentativasDevolve429() throws Exception {
        cadastrar("jwt.forca@example.com", Role.USER, true);
        for (int i = 0; i < 5; i++) {
            login("10.1.0.6", "jwt.forca@example.com", "Errada123").andExpect(status().isUnauthorized());
        }

        login("10.1.0.6", "jwt.forca@example.com", SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void sessaoDoNavegadorNaoUsaCsrfDoBearer() throws Exception {
        // Sem Bearer, escrita continua exigindo CSRF (a sessao do navegador segue protegida).
        api.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
