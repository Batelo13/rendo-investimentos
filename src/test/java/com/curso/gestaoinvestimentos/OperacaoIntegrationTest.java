package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.dto.SaldoDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperacaoIntegrationTest {

    private static final BigDecimal SALDO_INICIAL_TESTE = new BigDecimal("100000.00");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CarteiraRepository carteiraRepository;
    @Autowired
    private AcaoRepository acaoRepository;
    @Autowired
    private CorretoraRepository corretoraRepository;
    @Autowired
    private OperacaoRepository operacaoRepository;
    @Autowired
    private PosicaoAtualRepository posicaoAtualRepository;

    @AfterEach
    void limparBanco() {
        posicaoAtualRepository.deleteAll();
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Usuario cadastrarUsuario(String email, String senhaPlana, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setRole(role);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());
        Usuario salvo = usuarioRepository.save(usuario);

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(SALDO_INICIAL_TESTE);
        carteiraRepository.save(carteira);

        return salvo;
    }

    // Acao e Corretora sao inseridas direto via repository, nao via POST /acoes ou
    // POST /corretoras -- esses endpoints chamam APIs externas reais (cotacao,
    // CNPJ) e deixariam os testes lentos e flaky.
    private Acao cadastrarAcao(String ticker) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("120.00"));
        return acaoRepository.save(acao);
    }

    private Corretora cadastrarCorretora(boolean validadaNaCvm) {
        Corretora corretora = new Corretora();
        corretora.setCnpj("CNPJ-" + System.nanoTime());
        corretora.setNomeFantasia("Corretora Teste");
        corretora.setValidadaNaCvm(validadaNaCvm);
        return corretoraRepository.save(corretora);
    }

    private MockHttpSession logar(String email, String senhaPlana) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/login")
                        .param("username", email)
                        .param("password", senhaPlana))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test
    void deveRegistrarCompraERefletirNaPosicao() throws Exception {
        cadastrarUsuario("investidor@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("AAPL");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("investidor@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("100.00"));

        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/carteiras/me").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoes = objectMapper.readValue(resultado.getResponse().getContentAsString(), PosicaoDTO[].class);

        assertEquals(1, posicoes.length);
        assertEquals("AAPL", posicoes[0].acaoTicker());
        assertEquals(0, posicoes[0].quantidade().compareTo(new BigDecimal("10")));
        assertEquals(0, posicoes[0].precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void deveGravarPrecoMedioNaVendaEBloquearVendaADescoberto() throws Exception {
        cadastrarUsuario("vendedor@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("PETR4");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("vendedor@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO vendaADescoberto = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("15"), new BigDecimal("50.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaADescoberto)))
                .andExpect(status().isUnprocessableEntity());

        OperacaoRequestDTO vendaParcial = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("5"), new BigDecimal("50.00"));
        MvcResult resultadoVenda = mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaParcial)))
                .andExpect(status().isCreated())
                .andReturn();

        OperacaoResponseDTO operacaoVenda = objectMapper.readValue(
                resultadoVenda.getResponse().getContentAsString(), OperacaoResponseDTO.class);
        assertEquals(0, operacaoVenda.precoMedioNaVenda().compareTo(new BigDecimal("100.00")));
        assertEquals(0, operacaoVenda.lucroPrejuizoRealizado().compareTo(new BigDecimal("-250.00")));
    }

    @Test
    void deveBloquearOperacaoEmCorretoraNaoValidadaNaCvm() throws Exception {
        cadastrarUsuario("naovalidado@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("VALE3");
        Corretora corretora = cadastrarCorretora(false);
        MockHttpSession sessao = logar("naovalidado@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("100.00"));

        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void usuarioNaoAcessaCarteiraDeOutroUsuario() throws Exception {
        Usuario outro = cadastrarUsuario("outro@example.com", "senha1234", Role.USER);
        cadastrarUsuario("proprio@example.com", "senha1234", Role.USER);
        MockHttpSession sessao = logar("proprio@example.com", "senha1234");

        mockMvc.perform(get("/carteiras/" + outro.getId()).session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAcessaCarteiraDeQualquerUsuarioSomenteLeitura() throws Exception {
        Usuario usuarioComum = cadastrarUsuario("comum@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin@example.com", "senha1234", Role.ADMIN);
        MockHttpSession sessaoAdmin = logar("admin@example.com", "senha1234");

        MvcResult resultado = mockMvc.perform(get("/carteiras/" + usuarioComum.getId()).session(sessaoAdmin))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoes = objectMapper.readValue(resultado.getResponse().getContentAsString(), PosicaoDTO[].class);

        assertEquals(0, posicoes.length);
    }

    @Test
    void adminCancelaCompraUsuarioComumNaoConsegue() throws Exception {
        cadastrarUsuario("dono@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin2@example.com", "senha1234", Role.ADMIN);
        Acao acao = cadastrarAcao("ITUB4");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessaoDono = logar("dono@example.com", "senha1234");
        MockHttpSession sessaoAdmin = logar("admin2@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("30.00"));
        MvcResult resultado = mockMvc.perform(post("/operacoes")
                        .session(sessaoDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated())
                .andReturn();
        Long operacaoId = objectMapper.readValue(resultado.getResponse().getContentAsString(), OperacaoResponseDTO.class).id();

        mockMvc.perform(patch("/operacoes/" + operacaoId + "/cancelar").session(sessaoDono))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/operacoes/" + operacaoId + "/cancelar").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void cancelamentoBloqueadoQuandoDeixariaSaldoNegativo() throws Exception {
        cadastrarUsuario("dono2@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin3@example.com", "senha1234", Role.ADMIN);
        Acao acao = cadastrarAcao("MGLU3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessaoDono = logar("dono2@example.com", "senha1234");
        MockHttpSession sessaoAdmin = logar("admin3@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("30.00"));
        MvcResult resultadoCompra = mockMvc.perform(post("/operacoes")
                        .session(sessaoDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated())
                .andReturn();
        Long operacaoId = objectMapper.readValue(resultadoCompra.getResponse().getContentAsString(), OperacaoResponseDTO.class).id();

        OperacaoRequestDTO venda = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("8"), new BigDecimal("40.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessaoDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venda)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/operacoes/" + operacaoId + "/cancelar").session(sessaoAdmin))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void zerarPosicaoEComprarDeNovoReiniciaPrecoMedio() throws Exception {
        cadastrarUsuario("recomeco@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("WEGE3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("recomeco@example.com", "senha1234");

        OperacaoRequestDTO compra1 = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("10.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra1)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO vendeTudo = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("10"), new BigDecimal("10.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendeTudo)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO compra2 = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("5"), new BigDecimal("20.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra2)))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/carteiras/me").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoes = objectMapper.readValue(resultado.getResponse().getContentAsString(), PosicaoDTO[].class);

        assertEquals(1, posicoes.length);
        assertEquals(0, posicoes[0].quantidade().compareTo(new BigDecimal("5")));
        assertEquals(0, posicoes[0].precoMedio().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void deveRegistrarCompraFracionariaERefletirNaPosicao() throws Exception {
        cadastrarUsuario("fracionario@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("BOVA11");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("fracionario@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("0.5"), new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO vendaMaiorQueFracao = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("0.7"), new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaMaiorQueFracao)))
                .andExpect(status().isUnprocessableEntity());

        MvcResult resultado = mockMvc.perform(get("/carteiras/me").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoes = objectMapper.readValue(resultado.getResponse().getContentAsString(), PosicaoDTO[].class);

        assertEquals(1, posicoes.length);
        assertEquals(0, posicoes[0].quantidade().compareTo(new BigDecimal("0.5")));
    }

    @Test
    void adminReconstroiCachePosicaoAposDivergencia() throws Exception {
        Usuario dono = cadastrarUsuario("cachedono@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admincache@example.com", "senha1234", Role.ADMIN);
        Acao acao = cadastrarAcao("CACH3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessaoDono = logar("cachedono@example.com", "senha1234");
        MockHttpSession sessaoAdmin = logar("admincache@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("50.00"));
        mockMvc.perform(post("/operacoes").session(sessaoDono).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        MvcResult antes = mockMvc.perform(get("/carteiras/me").session(sessaoDono))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoesAntes = objectMapper.readValue(antes.getResponse().getContentAsString(), PosicaoDTO[].class);
        assertEquals(1, posicoesAntes.length);

        // Simula divergencia: apaga o cache direto pelo repository, por fora do
        // fluxo normal de escrita (que so acontece via registrar/cancelar).
        posicaoAtualRepository.deleteAll();

        MvcResult depoisDeApagar = mockMvc.perform(get("/carteiras/me").session(sessaoDono))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoesApagadas = objectMapper.readValue(depoisDeApagar.getResponse().getContentAsString(), PosicaoDTO[].class);
        assertEquals(0, posicoesApagadas.length);

        mockMvc.perform(patch("/carteiras/" + dono.getId() + "/reconstruir").session(sessaoAdmin))
                .andExpect(status().isOk());

        MvcResult depoisDeReconstruir = mockMvc.perform(get("/carteiras/me").session(sessaoDono))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoesRestauradas = objectMapper.readValue(depoisDeReconstruir.getResponse().getContentAsString(), PosicaoDTO[].class);
        assertEquals(1, posicoesRestauradas.length);
        assertEquals(0, posicoesRestauradas[0].quantidade().compareTo(new BigDecimal("10")));
    }

    @Test
    void usuarioComumNaoConsegueReconstruirCache() throws Exception {
        Usuario alvo = cadastrarUsuario("alvocache@example.com", "senha1234", Role.USER);
        cadastrarUsuario("comumcache@example.com", "senha1234", Role.USER);
        MockHttpSession sessaoComum = logar("comumcache@example.com", "senha1234");

        mockMvc.perform(patch("/carteiras/" + alvo.getId() + "/reconstruir").session(sessaoComum))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveBloquearCompraQuandoSaldoInsuficiente() throws Exception {
        cadastrarUsuario("semsaldo@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("CARO3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("semsaldo@example.com", "senha1234");

        // saldo inicial e 100000.00; 2000 x 100.00 = 200000.00, maior que o saldo disponivel
        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("2000"), new BigDecimal("100.00"));

        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void compraDescontaSaldoEVendaDevolve() throws Exception {
        cadastrarUsuario("saldodono@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("SALDO3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("saldodono@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        MvcResult apósCompra = mockMvc.perform(get("/carteiras/me/saldo").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        SaldoDTO saldoApósCompra = objectMapper.readValue(apósCompra.getResponse().getContentAsString(), SaldoDTO.class);
        assertEquals(0, saldoApósCompra.saldoDisponivel().compareTo(new BigDecimal("99000.00")));

        OperacaoRequestDTO venda = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, new BigDecimal("10"), new BigDecimal("120.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venda)))
                .andExpect(status().isCreated());

        MvcResult apósVenda = mockMvc.perform(get("/carteiras/me/saldo").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        SaldoDTO saldoApósVenda = objectMapper.readValue(apósVenda.getResponse().getContentAsString(), SaldoDTO.class);
        // 100000 - 1000 + 1200 = 100200
        assertEquals(0, saldoApósVenda.saldoDisponivel().compareTo(new BigDecimal("100200.00")));
    }
}
