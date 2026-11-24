package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.integration.DolarApiCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.model.Role;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
import com.curso.gestaoinvestimentos.util.CsrfTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pedido do professor (Modulo 11, STRIDE spoofing/information disclosure): um
 * usuario nao pode ver nem alterar dados de outra conta -- nem copiando o link
 * do dashboard, nem trocando o id numa rota da API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
class IsolamentoEntreContasIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private AcaoRepository acaoRepository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private OperacaoRepository operacaoRepository;
    @Autowired private PosicaoAtualRepository posicaoAtualRepository;
    @MockitoBean private DolarApiCambioClient cambioClient;

    private Usuario ana;
    private long operacaoDaAna;
    private MockHttpSession sessaoBruno;

    @BeforeEach
    void anaTemCarteiraComUmaCompra() throws Exception {
        ana = cadastrar("ana.isolamento@example.com");
        cadastrar("bruno.isolamento@example.com");

        Acao acao = new Acao();
        acao.setTicker("ISOL3");
        acao.setNomeEmpresa("Empresa Isolamento");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("50.00"));
        acao = acaoRepository.save(acao);
        Corretora corretora = new Corretora();
        corretora.setCnpj("CNPJ-" + System.nanoTime());
        corretora.setNomeFantasia("Corretora Isolamento");
        corretora.setValidadaNaCvm(true);
        corretora = corretoraRepository.save(corretora);

        MockHttpSession sessaoAna = logar("ana.isolamento@example.com");
        String resposta = mockMvc.perform(post("/operacoes").session(sessaoAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OperacaoRequestDTO(acao.getId(), corretora.getId(),
                                TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("50.00")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        operacaoDaAna = objectMapper.readTree(resposta).get("id").asLong();

        sessaoBruno = logar("bruno.isolamento@example.com");
    }

    @AfterEach
    void limpar() {
        posicaoAtualRepository.deleteAll();
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario: Usuario tenta abrir dados de outra conta pelo id")
    void todaRotaComIdDeOutraContaResponde403() throws Exception {
        long idAna = ana.getId();
        List<MockHttpServletRequestBuilder> rotas = List.of(
                get("/carteiras/" + idAna),
                get("/carteiras/" + idAna + "/operacoes"),
                patch("/carteiras/" + idAna + "/reconstruir"),
                get("/usuarios/" + idAna),
                patch("/usuarios/" + idAna + "/bloquear"),
                patch("/usuarios/" + idAna + "/desbloquear"),
                patch("/operacoes/" + operacaoDaAna + "/cancelar"));

        for (MockHttpServletRequestBuilder rota : rotas) {
            mockMvc.perform(rota.session(sessaoBruno)).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Scenario: Link do dashboard de outra pessoa")
    void linkDoDashboardMostraSoAPropriaCarteira() throws Exception {
        // O link do dashboard nao carrega id: a pagina busca os dados da sessao.
        mockMvc.perform(get("/dashboard").session(sessaoBruno)).andExpect(status().isOk());
        mockMvc.perform(get("/carteiras/me").session(sessaoBruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/carteiras/me/operacoes").session(sessaoBruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // Sem sessao: o link leva ao login, e a API nao responde dado nenhum.
        mockMvc.perform(get("/dashboard")).andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/carteiras/me")).andExpect(status().isUnauthorized());
    }

    // Controle positivo: os dados da Ana existem (senao a carteira vazia do
    // Bruno nao provaria isolamento nenhum).
    @Test
    void donaDaCarteiraVeASuaCompra() throws Exception {
        mockMvc.perform(get("/carteiras/me").session(logar("ana.isolamento@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acaoTicker").value("ISOL3"));
    }

    private Usuario cadastrar(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Isolamento");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode("Senha1234"));
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());
        Usuario salvo = usuarioRepository.save(usuario);
        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(new BigDecimal("100000.00"));
        carteiraRepository.save(carteira);
        return salvo;
    }

    private MockHttpSession logar(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login").param("username", email).param("password", "Senha1234"))
                .andReturn().getRequest().getSession(false);
    }
}
