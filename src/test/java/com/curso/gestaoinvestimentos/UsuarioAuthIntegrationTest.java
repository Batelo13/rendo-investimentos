package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.UsuarioRequestDTO;
import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.TipoCodigo;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.EmailVerificationCodeRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private EmailVerificationCodeRepository codigoRepository;

    // H2 em memoria e recriado uma vez por contexto Spring, nao por teste -
    // sem isso, cadastros de um teste vazam pro proximo (ex: email duplicado).
    // Carteira e criada automaticamente no cadastro (FK obrigatoria pra
    // usuarios), por isso precisa ser apagada antes.
    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        codigoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // emailVerified=true aqui de proposito: este helper simula uma conta ja
    // provisionada (usada pelos testes de login/autorizacao), nao o fluxo de
    // cadastro em si -- esse fluxo tem cobertura propria mais abaixo.
    private Usuario cadastrarUsuario(String email, String senhaPlana, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setRole(role);
        usuario.setAtivo(true);
        usuario.setEmailVerified(true);
        usuario.setDataCadastro(LocalDate.now());
        return usuarioRepository.save(usuario);
    }

    @Test
    void deveCadastrarUsuarioPublicamenteSemAutenticacao() throws Exception {
        String cpf = CpfTestFixtures.proximoCpfValido();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Maria Silva", "maria@example.com", cpf, "senha1234");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.emailVerificationRequired").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty());

        Usuario salvo = usuarioRepository.findByEmail("maria@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, salvo.getEmailVerified());
    }

    @Test
    void deveRejeitarCadastroComCpfInvalido() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "Joao Invalido", "joao.invalido@example.com", "11144477736", "senha1234");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarCadastroComCpfDuplicado() throws Exception {
        String cpf = CpfTestFixtures.proximoCpfValido();
        UsuarioRequestDTO primeiro = new UsuarioRequestDTO(
                "Primeiro Usuario", "primeiro.cpf@example.com", cpf, "senha1234");
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeiro)))
                .andExpect(status().isAccepted());

        UsuarioRequestDTO segundo = new UsuarioRequestDTO(
                "Segundo Usuario", "segundo.cpf@example.com", cpf, "senha1234");
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(segundo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe um usuario cadastrado com o CPF " + cpf));
    }

    @Test
    void deveBloquearAcessoSemSessao() throws Exception {
        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveFazerLoginComCredenciaisValidas() throws Exception {
        cadastrarUsuario("login.valido@example.com", "senha1234", Role.USER);

        mockMvc.perform(post("/login")
                        .param("username", "login.valido@example.com")
                        .param("password", "senha1234"))
                .andExpect(status().isFound());
    }

    @Test
    void deveRejeitarLoginComCredenciaisInvalidas() throws Exception {
        cadastrarUsuario("login.invalido@example.com", "senha1234", Role.USER);

        mockMvc.perform(post("/login")
                        .param("username", "login.invalido@example.com")
                        .param("password", "senhaErrada"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login?error")));
    }

    @Test
    void devePermitirAcessoAutorizadoAposLogin() throws Exception {
        Usuario usuario = cadastrarUsuario("autorizado@example.com", "senha1234", Role.USER);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "autorizado@example.com")
                        .param("password", "senha1234"))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/usuarios/" + usuario.getId()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("autorizado@example.com"));
    }

    @Test
    void deveBloquearAcessoPorRole() throws Exception {
        cadastrarUsuario("usuario.comum@example.com", "senha1234", Role.USER);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "usuario.comum@example.com")
                        .param("password", "senha1234"))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/usuarios").session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveInvalidarSessaoAoFazerLogout() throws Exception {
        Usuario usuario = cadastrarUsuario("logout@example.com", "senha1234", Role.USER);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "logout@example.com")
                        .param("password", "senha1234"))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(sessao))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/usuarios/" + usuario.getId()).session(sessao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void telaDeLoginAposLogoutNaoExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/login?logout"))
                .andExpect(status().isOk());
    }

    private MockHttpSession logar(String email, String senhaPlana) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/login")
                        .param("username", email)
                        .param("password", senhaPlana))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test
    void adminBloqueiaUsuarioEImpedeLoginSubsequente() throws Exception {
        Usuario alvo = cadastrarUsuario("bloqueado@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin.bloqueio@example.com", "senha1234", Role.ADMIN);
        MockHttpSession sessaoAdmin = logar("admin.bloqueio@example.com", "senha1234");

        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/bloquear").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(post("/login")
                        .param("username", "bloqueado@example.com")
                        .param("password", "senha1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login?error")));
    }

    @Test
    void usuarioComumNaoConsegueBloquearOutroUsuario() throws Exception {
        Usuario alvo = cadastrarUsuario("alvo@example.com", "senha1234", Role.USER);
        cadastrarUsuario("comum.bloqueio@example.com", "senha1234", Role.USER);
        MockHttpSession sessaoComum = logar("comum.bloqueio@example.com", "senha1234");

        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/bloquear").session(sessaoComum))
                .andExpect(status().isForbidden());
    }

    @Test
    void bloquearUsuarioJaBloqueadoRetorna422() throws Exception {
        Usuario alvo = cadastrarUsuario("jabloqueado@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin.duplo@example.com", "senha1234", Role.ADMIN);
        MockHttpSession sessaoAdmin = logar("admin.duplo@example.com", "senha1234");

        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/bloquear").session(sessaoAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/bloquear").session(sessaoAdmin))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminDesbloqueiaUsuarioERestauraLogin() throws Exception {
        Usuario alvo = cadastrarUsuario("desbloquear@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin.desbloqueio@example.com", "senha1234", Role.ADMIN);
        MockHttpSession sessaoAdmin = logar("admin.desbloqueio@example.com", "senha1234");

        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/bloquear").session(sessaoAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/usuarios/" + alvo.getId() + "/desbloquear").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        mockMvc.perform(post("/login")
                        .param("username", "desbloquear@example.com")
                        .param("password", "senha1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.not(org.hamcrest.Matchers.endsWith("/login?error"))));
    }

    @Test
    void cadastroTradicionalGeraCodigoDeVerificacaoPendente() throws Exception {
        String cpf = CpfTestFixtures.proximoCpfValido();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Pendente Verificacao", "pendente@example.com", cpf, "senha1234");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());

        Usuario salvo = usuarioRepository.findByEmail("pendente@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, salvo.getEmailVerified());
        org.junit.jupiter.api.Assertions.assertTrue(
                codigoRepository.findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc(salvo, TipoCodigo.VERIFICACAO_EMAIL).isPresent());
    }

    @Test
    void loginBloqueadoParaContaComEmailNaoVerificadoComRedirectDistinto() throws Exception {
        Usuario naoVerificado = new Usuario();
        naoVerificado.setNome("Nao Verificado");
        naoVerificado.setEmail("naoverificado@example.com");
        naoVerificado.setCpf(CpfTestFixtures.proximoCpfValido());
        naoVerificado.setSenha(passwordEncoder.encode("senha1234"));
        naoVerificado.setRole(Role.USER);
        naoVerificado.setAtivo(true);
        naoVerificado.setEmailVerified(false);
        naoVerificado.setDataCadastro(LocalDate.now());
        usuarioRepository.save(naoVerificado);

        mockMvc.perform(post("/login")
                        .param("username", "naoverificado@example.com")
                        .param("password", "senha1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login?erro=email-nao-verificado")));
    }
}
