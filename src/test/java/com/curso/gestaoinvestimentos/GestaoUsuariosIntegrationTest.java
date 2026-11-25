package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import com.curso.gestaoinvestimentos.util.CsrfTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Aula 14: gestao de usuarios pelo ADMIN (criar conta, papel, senha, sem autobloqueio). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
class GestaoUsuariosIntegrationTest {

    private static final String ADMIN = "gestor@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Usuario cadastrar(String email, Role role) {
        Usuario u = new Usuario();
        u.setNome("Conta " + email);
        u.setEmail(email);
        u.setCpf(CpfTestFixtures.proximoCpfValido());
        u.setSenha(passwordEncoder.encode("Senha1234"));
        u.setRole(role);
        u.setAtivo(true);
        u.setEmailVerified(true);
        u.setDataCadastro(LocalDate.now());
        return usuarioRepository.save(u);
    }

    private ResultActions loginJwt(String ip, String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
                .with(req -> { req.setRemoteAddr(ip); return req; }));
    }

    private ResultActions comoAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.with(user(ADMIN).roles("ADMIN")));
    }

    @Test
    @DisplayName("Scenario: Admin cria conta que ja pode entrar")
    void adminCriaContaQueJaPodeEntrar() throws Exception {
        comoAdmin(post("/usuarios/admin").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Nova Pessoa","email":"nova@example.com","cpf":"%s","senha":"Senha123","role":"USER"}
                """.formatted(CpfTestFixtures.proximoCpfValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist());

        // Ja verificada e com carteira: entra direto.
        loginJwt("10.2.0.1", "nova@example.com", "Senha123").andExpect(status().isOk());
        Usuario salvo = usuarioRepository.findByEmail("nova@example.com").orElseThrow();
        assertThat(carteiraRepository.findByUsuarioId(salvo.getId())).isPresent();
    }

    @Test
    @DisplayName("Scenario: Email repetido")
    void emailRepetidoDevolve409() throws Exception {
        cadastrar("repetido@example.com", Role.USER);

        comoAdmin(post("/usuarios/admin").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Outra","email":"repetido@example.com","cpf":"%s","senha":"Senha123","role":"USER"}
                """.formatted(CpfTestFixtures.proximoCpfValido())))
                .andExpect(status().isConflict());
    }

    @Test
    void criarContaComSenhaFracaDevolve400() throws Exception {
        comoAdmin(post("/usuarios/admin").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Fraca","email":"fraca@example.com","cpf":"%s","senha":"senha123","role":"USER"}
                """.formatted(CpfTestFixtures.proximoCpfValido())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("maiuscula e minuscula")));
    }

    @Test
    @DisplayName("Scenario: Promover usuario")
    void adminPromoveUsuario() throws Exception {
        Usuario alvo = cadastrar("promovido@example.com", Role.USER);

        comoAdmin(patch("/usuarios/{id}/papel", alvo.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Scenario: Proprio papel")
    void adminNaoMudaOProprioPapel() throws Exception {
        Usuario eu = cadastrar(ADMIN, Role.ADMIN);
        cadastrar("outro.admin@example.com", Role.ADMIN);

        comoAdmin(patch("/usuarios/{id}/papel", eu.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
                .andExpect(status().is(422));
        assertThat(usuarioRepository.findById(eu.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Scenario: Senha redefinida")
    void senhaRedefinidaTrocaOLogin() throws Exception {
        Usuario alvo = cadastrar("trocasenha@example.com", Role.USER);

        comoAdmin(put("/usuarios/{id}/senha", alvo.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senha\":\"NovaSenha9\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        loginJwt("10.2.0.2", "trocasenha@example.com", "Senha1234").andExpect(status().isUnauthorized());
        loginJwt("10.2.0.2", "trocasenha@example.com", "NovaSenha9").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario: Senha fraca recusada")
    void senhaFracaNaRedefinicaoDevolve400() throws Exception {
        Usuario alvo = cadastrar("semtroca@example.com", Role.USER);
        String hashAntes = alvo.getSenha();

        comoAdmin(put("/usuarios/{id}/senha", alvo.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senha\":\"fraca123\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findById(alvo.getId()).orElseThrow().getSenha()).isEqualTo(hashAntes);
    }

    @Test
    @DisplayName("Scenario: Autobloqueio")
    void adminNaoSeBloqueia() throws Exception {
        Usuario eu = cadastrar(ADMIN, Role.ADMIN);

        comoAdmin(patch("/usuarios/{id}/bloquear", eu.getId())).andExpect(status().is(422));
        assertThat(usuarioRepository.findById(eu.getId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Scenario: USER barrado")
    void userRecebe403NasRotasDeGestao() throws Exception {
        Usuario alvo = cadastrar("alvo.gestao@example.com", Role.USER);
        var comum = user("comum@example.com").roles("USER");

        mockMvc.perform(post("/usuarios/admin").with(comum).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"X","email":"x@example.com","cpf":"%s","senha":"Senha123","role":"ADMIN"}
                """.formatted(CpfTestFixtures.proximoCpfValido()))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/usuarios/{id}/papel", alvo.getId()).with(comum).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/usuarios/{id}/senha", alvo.getId()).with(comum).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senha\":\"NovaSenha9\"}")).andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByEmail("x@example.com")).isEmpty();
    }
}
