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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Painel do administrador: estatisticas das contas e area exclusiva no dashboard. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
class AdminEstatisticasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CarteiraRepository carteiraRepository;

    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void cadastrar(String email, Role role, boolean ativo, Boolean verificado, LocalDate cadastro) {
        Usuario usuario = new Usuario();
        usuario.setNome("Conta " + email);
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha("{noop}x");
        usuario.setRole(role);
        usuario.setAtivo(ativo);
        usuario.setEmailVerified(verificado);
        usuario.setDataCadastro(cadastro);
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Scenario: Contagens refletem o banco")
    void estatisticasContamAsContas() throws Exception {
        cadastrar("chefe@example.com", Role.ADMIN, true, true, LocalDate.now());
        cadastrar("bloqueado@example.com", Role.USER, false, true, LocalDate.now().minusDays(5));
        cadastrar("novo@example.com", Role.USER, true, false, LocalDate.now().minusDays(30));
        cadastrar("antigo@example.com", Role.USER, true, true, LocalDate.now().minusDays(31));

        mockMvc.perform(get("/usuarios/estatisticas").with(user("chefe@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.ativas").value(3))
                .andExpect(jsonPath("$.bloqueadas").value(1))
                .andExpect(jsonPath("$.emailNaoVerificado").value(1))
                .andExpect(jsonPath("$.administradores").value(1))
                // Limite: 30 dias atras conta, 31 nao.
                .andExpect(jsonPath("$.novasUltimos30Dias").value(3));
    }

    @Test
    @DisplayName("Scenario: USER barrado")
    void userNaoVeEstatisticas() throws Exception {
        mockMvc.perform(get("/usuarios/estatisticas").with(user("comum@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteSemLoginRecebe401() throws Exception {
        mockMvc.perform(get("/usuarios/estatisticas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario: Admin ve a area")
    void dashboardMostraAreaAdminParaAdmin() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("chefe@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-view=\"admin\"")))
                .andExpect(content().string(containsString("id=\"view-admin\"")))
                .andExpect(content().string(containsString("Administrador")));
    }

    @Test
    @DisplayName("Scenario: Usuario comum nao ve")
    void dashboardEscondeAreaAdminDoUser() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("comum@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-view=\"admin\""))))
                .andExpect(content().string(not(containsString("id=\"view-admin\""))))
                .andExpect(content().string(not(containsString("seloAdmin"))));
    }
}
