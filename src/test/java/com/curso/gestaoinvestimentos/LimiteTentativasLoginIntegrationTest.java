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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Modulo 11: limite de tentativas de login por formulario. Cada teste usa um IP
 * proprio (o contador e por IP e vive no contexto Spring compartilhado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
class LimiteTentativasLoginIntegrationTest {

    private static final String SENHA = "Senha1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CarteiraRepository carteiraRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void cadastrar(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        usuario.setEmailVerified(true);
        usuario.setDataCadastro(LocalDate.now());
        usuarioRepository.save(usuario);
    }

    private ResultActions login(String ip, String email, String senha) throws Exception {
        return mockMvc.perform(post("/login").param("username", email).param("password", senha)
                .with(req -> { req.setRemoteAddr(ip); return req; }));
    }

    @Test
    @DisplayName("Scenario: Bloqueio apos 5 falhas seguidas")
    void bloqueiaAposCincoFalhasMesmoComSenhaCerta() throws Exception {
        cadastrar("alvo@example.com");
        for (int i = 0; i < 5; i++) {
            login("10.0.0.1", "alvo@example.com", "errada").andExpect(header().string("Location", endsWith("/login?error")));
        }
        login("10.0.0.1", "alvo@example.com", SENHA)
                .andExpect(header().string("Location", endsWith("/login?erro=muitas-tentativas")));
        mockMvc.perform(get("/login").param("erro", "muitas-tentativas"))
                .andExpect(content().string(containsString("Muitas tentativas de login")))
                .andExpect(content().string(not(containsString("Confirme seu e-mail antes de entrar"))));
        // Email com caixa diferente conta como o mesmo alvo.
        login("10.0.0.1", "ALVO@example.com", SENHA)
                .andExpect(header().string("Location", endsWith("/login?erro=muitas-tentativas")));
    }

    @Test
    @DisplayName("Scenario: Bloqueio nao afeta o dono da conta em outro IP")
    void outroIpContinuaEntrando() throws Exception {
        cadastrar("dono@example.com");
        for (int i = 0; i < 5; i++) {
            login("10.0.0.2", "dono@example.com", "errada");
        }
        login("10.0.0.3", "dono@example.com", SENHA).andExpect(header().string("Location", endsWith("/dashboard")));
    }

    @Test
    @DisplayName("Scenario: Login certo zera as falhas")
    void sucessoZeraContador() throws Exception {
        cadastrar("distraido@example.com");
        for (int i = 0; i < 4; i++) {
            login("10.0.0.4", "distraido@example.com", "errada");
        }
        login("10.0.0.4", "distraido@example.com", SENHA).andExpect(header().string("Location", endsWith("/dashboard")));
        for (int i = 0; i < 4; i++) {
            login("10.0.0.4", "distraido@example.com", "errada");
        }
        login("10.0.0.4", "distraido@example.com", SENHA).andExpect(header().string("Location", endsWith("/dashboard")));
    }

    @Test
    @DisplayName("Scenario: Muitas falhas de um IP em emails diferentes")
    void bloqueiaIpQueTestaMuitosEmails() throws Exception {
        cadastrar("vitima@example.com");
        for (int i = 0; i < 20; i++) {
            login("10.0.0.5", "inexistente" + i + "@example.com", "errada");
        }
        login("10.0.0.5", "vitima@example.com", SENHA)
                .andExpect(header().string("Location", endsWith("/login?erro=muitas-tentativas")));
    }
}
