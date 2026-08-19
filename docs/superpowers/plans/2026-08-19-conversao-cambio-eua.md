# Conversão de Câmbio USD→BRL para Ações Americanas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir o cálculo de saldo pra descontar/creditar o valor real em reais (não o número bruto em dólar) em operações de ações americanas, e mostrar o valor convertido em reais ao lado do valor em dólar no catálogo de ações, no painel de posições e no modal de compra/venda.

**Architecture:** A taxa de câmbio é capturada no momento de cada operação e gravada de forma imutável em `Operacao.taxaCambio` (mesma filosofia já usada pro histórico de operações — fonte da verdade determinística). `SaldoCalculator` passa a multiplicar por essa taxa. Pro lado de exibição, um único campo novo (`AcaoResponseDTO.cotacaoAtualBRL`) carrega a cotação já convertida; o frontend deriva a taxa implícita desse campo e converte localmente qualquer outro valor da mesma ação, sem precisar de campos novos em `PosicaoDTO`/`OperacaoResponseDTO` nem de um endpoint dedicado de câmbio.

**Tech Stack:** Spring Boot (Java 17), TwelveData API (mesma chave já usada pra cotação de ações EUA), JUnit 5 + Mockito, JS puro (sem libs novas).

Spec de referência: `docs/superpowers/specs/2026-08-19-conversao-cambio-eua-design.md`.

---

### Task 1: Campo `taxaCambio` em `Operacao`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/model/Operacao.java`

- [ ] **Step 1: Add the field**

Em `src/main/java/com/curso/gestaoinvestimentos/model/Operacao.java`, atualmente (linhas 44-48):

```java
    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataHora;
```

Substituir por (novo campo `taxaCambio` entre `precoUnitario` e `dataHora`):

```java
    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private BigDecimal taxaCambio;

    @Column(nullable = false)
    private LocalDateTime dataHora;
```

- [ ] **Step 2: Add the getter/setter**

Atualmente (linhas 110-118):

```java
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
```

Substituir por:

```java
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getTaxaCambio() {
        return taxaCambio;
    }

    public void setTaxaCambio(BigDecimal taxaCambio) {
        this.taxaCambio = taxaCambio;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
```

- [ ] **Step 3: Compile to confirm no syntax errors**

Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS. `PosicaoCalculator`/`PosicaoCalculatorTest` não usam `taxaCambio`, então nada mais quebra ainda — `SaldoCalculator` (que vai usar) é corrigido na Task 2.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/model/Operacao.java
git commit -m "feat(operacao): adiciona campo taxaCambio ao modelo"
```

---

### Task 2: `SaldoCalculator` usa a taxa de câmbio (TDD)

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/SaldoCalculator.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/service/SaldoCalculatorTest.java`

- [ ] **Step 1: Update the test helper and write the failing test**

Substituir o arquivo inteiro `src/test/java/com/curso/gestaoinvestimentos/service/SaldoCalculatorTest.java` por:

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaldoCalculatorTest {

    private Operacao operacao(TipoOperacao tipo, String quantidade, String precoUnitario) {
        return operacao(tipo, quantidade, precoUnitario, "1");
    }

    private Operacao operacao(TipoOperacao tipo, String quantidade, String precoUnitario, String taxaCambio) {
        Operacao operacao = new Operacao();
        operacao.setTipo(tipo);
        operacao.setQuantidade(new BigDecimal(quantidade));
        operacao.setPrecoUnitario(new BigDecimal(precoUnitario));
        operacao.setTaxaCambio(new BigDecimal(taxaCambio));
        return operacao;
    }

    @Test
    void semOperacoesSaldoEIgualAoInicial() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of());

        assertEquals(0, saldo.compareTo(new BigDecimal("100000.00")));
    }

    @Test
    void compraDescontaDoSaldo() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00")
        ));

        assertEquals(0, saldo.compareTo(new BigDecimal("99000.00")));
    }

    @Test
    void vendaSomaAoSaldo() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00"),
                operacao(TipoOperacao.VENDA, "5", "150.00")
        ));

        // 100000 - (10*100) + (5*150) = 100000 - 1000 + 750 = 99750
        assertEquals(0, saldo.compareTo(new BigDecimal("99750.00")));
    }

    @Test
    void compraEmAcaoEuaDescontaValorConvertidoPelaTaxaDeCambio() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00", "5.00")
        ));

        // 100000 - (10*100*5) = 100000 - 5000 = 95000
        assertEquals(0, saldo.compareTo(new BigDecimal("95000.00")));
    }

    @Test
    void vendaEmAcaoEuaSomaValorConvertidoPelaTaxaDeCambio() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00", "5.00"),
                operacao(TipoOperacao.VENDA, "10", "120.00", "5.50")
        ));

        // 100000 - (10*100*5) + (10*120*5.5) = 100000 - 5000 + 6600 = 101600
        assertEquals(0, saldo.compareTo(new BigDecimal("101600.00")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test "-Dtest=SaldoCalculatorTest"`
Expected: FAIL — os 2 novos testes (`compraEmAcaoEuaDescontaValorConvertidoPelaTaxaDeCambio`, `vendaEmAcaoEuaSomaValorConvertidoPelaTaxaDeCambio`) falham porque `SaldoCalculator` ainda ignora `taxaCambio`. Os 3 testes antigos continuam passando (taxa "1" não muda o resultado).

- [ ] **Step 3: Fix `SaldoCalculator`**

Substituir o arquivo inteiro `src/main/java/com/curso/gestaoinvestimentos/service/SaldoCalculator.java` por:

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calcula o saldo virtual disponivel a partir do saldo inicial da carteira e
 * do historico de operacoes em ordem cronologica. Mesmo raciocinio do
 * PosicaoCalculator: Operacao e a fonte da verdade, saldo e um valor
 * derivado, nunca guardado separadamente -- cancelar uma compra "devolve" o
 * saldo automaticamente, so por ela sair do historico ATIVA usado aqui.
 *
 * taxaCambio e sempre 1 pra acoes BRASIL e a taxa USD->BRL vigente no
 * momento de cada operacao pra acoes EUA (gravada na propria Operacao,
 * nunca recalculada com a taxa "de agora" -- mesma garantia de historico
 * imutavel/deterministico ja usada pro preco).
 */
public class SaldoCalculator {

    public static BigDecimal calcular(BigDecimal saldoInicial, List<Operacao> operacoesEmOrdemCronologica) {
        BigDecimal saldo = saldoInicial;

        for (Operacao operacao : operacoesEmOrdemCronologica) {
            BigDecimal valor = operacao.getPrecoUnitario()
                    .multiply(operacao.getQuantidade())
                    .multiply(operacao.getTaxaCambio());
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                saldo = saldo.subtract(valor);
            } else {
                saldo = saldo.add(valor);
            }
        }

        return saldo;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd test "-Dtest=SaldoCalculatorTest"`
Expected: PASS — 5 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/SaldoCalculator.java src/test/java/com/curso/gestaoinvestimentos/service/SaldoCalculatorTest.java
git commit -m "fix(saldo): desconta/credita o valor convertido pela taxa de cambio, nao o bruto"
```

---

### Task 3: `TwelveDataCambioClient` (novo)

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/integration/TwelveDataCambioClient.java`

Sem teste dedicado: `TwelveDataCotacaoProvider` (o cliente já existente mais parecido, mesmo estilo de chamada HTTP) também não tem teste próprio nesse projeto — é testado indiretamente via mock nos services/testes de integração que o usam (Task 4).

- [ ] **Step 1: Create the client**

```java
package com.curso.gestaoinvestimentos.integration;

import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Isola a busca da taxa de cambio USD->BRL, mesma API/chave ja usada pra
 * cotacao de acoes EUA (TwelveDataCotacaoProvider). Sem interface Strategy
 * aqui -- diferente de CotacaoProvider (que genuinamente escolhe entre
 * BRASIL/EUA), so existe uma fonte de cambio, entao uma interface seria
 * abstracao sem necessidade.
 */
@Component
public class TwelveDataCambioClient {

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataCambioClient(@Value("${twelvedata.api.key:demo}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.twelvedata.com")
                .requestFactory(requestFactory)
                .build();
    }

    public BigDecimal buscarTaxaUsdParaBrl() {
        try {
            RespostaExchangeRate resposta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/exchange_rate")
                            .queryParam("symbol", "USD/BRL")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(RespostaExchangeRate.class);

            if (resposta == null || resposta.rate() == null) {
                throw new ServicoExternoIndisponivelException("Nao foi possivel obter a cotacao USD/BRL na Twelve Data");
            }

            return resposta.rate();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ServicoExternoIndisponivelException(
                    "Twelve Data recusou a chave de API. Configure TWELVEDATA_API_KEY com uma chave valida.");
        } catch (HttpClientErrorException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel obter a cotacao USD/BRL na Twelve Data");
        } catch (RestClientException ex) {
            throw new ServicoExternoIndisponivelException("Nao foi possivel consultar a cotacao USD/BRL na Twelve Data");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaExchangeRate(BigDecimal rate) {
    }
}
```

- [ ] **Step 2: Compile to confirm no syntax errors**

Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS. Nada ainda referencia essa classe (fica pras Tasks 4 e 5).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/integration/TwelveDataCambioClient.java
git commit -m "feat(cambio): adiciona cliente de taxa de cambio USD/BRL via Twelve Data"
```

---

### Task 4: `OperacaoService` grava e usa a taxa de câmbio

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

Em `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`, adicionar aos imports (perto dos outros imports de `com.curso.gestaoinvestimentos.*`, ordem exata não importa — `SaldoDTO` já está importado no arquivo, só falta este):

```java
import com.curso.gestaoinvestimentos.integration.TwelveDataCambioClient;
```

E aos imports estáticos/de teste:

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
```

Adicionar o campo mockado, junto dos outros `@Autowired` no topo da classe:

```java
    @MockitoBean
    private TwelveDataCambioClient cambioClient;
```

Adicionar um novo helper, logo depois do método `cadastrarAcao` existente:

```java
    private Acao cadastrarAcaoEua(String ticker) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.EUA);
        acao.setMoeda("USD");
        acao.setCotacaoAtual(new BigDecimal("100.00"));
        return acaoRepository.save(acao);
    }
```

E o novo teste, em qualquer lugar entre os outros métodos `@Test` da classe:

```java
    @Test
    void compraDeAcaoEuaGravaTaxaDeCambioEDescontaSaldoConvertido() throws Exception {
        cadastrarUsuario("investidor.eua@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcaoEua("AAPL");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("investidor.eua@example.com", "senha1234");

        when(cambioClient.buscarTaxaUsdParaBrl()).thenReturn(new BigDecimal("5.00"));

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, new BigDecimal("10"), new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/carteiras/me/saldo").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        SaldoDTO saldo = objectMapper.readValue(resultado.getResponse().getContentAsString(), SaldoDTO.class);

        // 100000 - (10 * 100 * 5.00) = 100000 - 5000 = 95000
        assertEquals(0, saldo.saldoDisponivel().compareTo(new BigDecimal("95000.00")));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test "-Dtest=OperacaoIntegrationTest#compraDeAcaoEuaGravaTaxaDeCambioEDescontaSaldoConvertido"`
Expected: FAIL — a compra é registrada, mas o saldo descontado é `100000 - (10*100*1)` (já que `Operacao.taxaCambio` nunca é preenchida com a taxa de verdade ainda), então `saldoDisponivel` fica `99000.00`, não `95000.00`. (Se o teste falhar por outro motivo, como erro de compilação por causa de um import errado, resolva o import antes de seguir — não pule esse passo.)

- [ ] **Step 3: Wire the client into `OperacaoService`**

Em `src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java`, adicionar aos imports:

```java
import com.curso.gestaoinvestimentos.integration.TwelveDataCambioClient;
import com.curso.gestaoinvestimentos.model.Mercado;
```

Adicionar o novo campo e atualizar o construtor (atualmente):

```java
    private final OperacaoRepository operacaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PosicaoCacheService posicaoCacheService;

    public OperacaoService(OperacaoRepository operacaoRepository, CarteiraRepository carteiraRepository,
                            AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            UsuarioRepository usuarioRepository, PosicaoCacheService posicaoCacheService) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
    }
```

Substituir por:

```java
    private final OperacaoRepository operacaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PosicaoCacheService posicaoCacheService;
    private final TwelveDataCambioClient cambioClient;

    public OperacaoService(OperacaoRepository operacaoRepository, CarteiraRepository carteiraRepository,
                            AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            UsuarioRepository usuarioRepository, PosicaoCacheService posicaoCacheService,
                            TwelveDataCambioClient cambioClient) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
        this.cambioClient = cambioClient;
    }
```

- [ ] **Step 4: Fetch and apply the rate in `registrar()`**

No mesmo arquivo, o método `registrar()` atualmente tem este trecho (logo após a checagem de CVM da corretora):

```java
        if (!Boolean.TRUE.equals(corretora.getValidadaNaCvm())) {
            throw new RegraDeNegocioException("Corretora " + corretora.getNomeFantasia() + " nao e validada na CVM");
        }

        if (dto.tipo() == TipoOperacao.COMPRA) {
            List<Operacao> historicoCarteira = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(
                    carteira.getId(), StatusOperacao.ATIVA);
            BigDecimal saldoDisponivel = SaldoCalculator.calcular(carteira.getSaldoInicial(), historicoCarteira);
            BigDecimal custoCompra = dto.precoUnitario().multiply(dto.quantidade());
            if (custoCompra.compareTo(saldoDisponivel) > 0) {
                throw new RegraDeNegocioException(
                        "Saldo em conta insuficiente: disponivel R$ " + saldoDisponivel
                                + ", tentando comprar R$ " + custoCompra);
            }
        }
```

Substituir por:

```java
        if (!Boolean.TRUE.equals(corretora.getValidadaNaCvm())) {
            throw new RegraDeNegocioException("Corretora " + corretora.getNomeFantasia() + " nao e validada na CVM");
        }

        BigDecimal taxaCambio = acao.getMercado() == Mercado.EUA
                ? cambioClient.buscarTaxaUsdParaBrl()
                : BigDecimal.ONE;

        if (dto.tipo() == TipoOperacao.COMPRA) {
            List<Operacao> historicoCarteira = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(
                    carteira.getId(), StatusOperacao.ATIVA);
            BigDecimal saldoDisponivel = SaldoCalculator.calcular(carteira.getSaldoInicial(), historicoCarteira);
            BigDecimal custoCompra = dto.precoUnitario().multiply(dto.quantidade()).multiply(taxaCambio);
            if (custoCompra.compareTo(saldoDisponivel) > 0) {
                throw new RegraDeNegocioException(
                        "Saldo em conta insuficiente: disponivel R$ " + saldoDisponivel
                                + ", tentando comprar R$ " + custoCompra);
            }
        }
```

Logo abaixo, ainda no mesmo método, atualmente:

```java
        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setCorretora(corretora);
        operacao.setTipo(dto.tipo());
        operacao.setQuantidade(dto.quantidade());
        operacao.setPrecoUnitario(dto.precoUnitario());
        operacao.setDataHora(LocalDateTime.now());
        operacao.setStatus(StatusOperacao.ATIVA);
```

Substituir por (adiciona `setTaxaCambio`):

```java
        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setCorretora(corretora);
        operacao.setTipo(dto.tipo());
        operacao.setQuantidade(dto.quantidade());
        operacao.setPrecoUnitario(dto.precoUnitario());
        operacao.setTaxaCambio(taxaCambio);
        operacao.setDataHora(LocalDateTime.now());
        operacao.setStatus(StatusOperacao.ATIVA);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\mvnw.cmd test "-Dtest=OperacaoIntegrationTest"`
Expected: PASS — todos os cenários (os já existentes + o novo) verdes. Os cenários com ação BRASIL continuam batendo porque `taxaCambio` fica `1` pra elas, sem nenhuma chamada a `cambioClient`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "feat(operacao): grava e aplica a taxa de cambio em compras/vendas de acoes EUA"
```

---

### Task 5: `AcaoResponseDTO.cotacaoAtualBRL` + `AcaoService`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/dto/AcaoResponseDTO.java`
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/AcaoService.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/service/AcaoServiceTest.java` (novo)

- [ ] **Step 1: Add the field to `AcaoResponseDTO`**

Substituir o arquivo inteiro `src/main/java/com/curso/gestaoinvestimentos/dto/AcaoResponseDTO.java` por:

```java
package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Mercado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcaoResponseDTO(
        Long id,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        String moeda,
        BigDecimal cotacaoAtual,
        BigDecimal cotacaoAtualBRL,
        LocalDateTime dataHoraCotacao
) {
}
```

Isso quebra a compilação de `AcaoService.toResponseDTO` (constrói `AcaoResponseDTO` com 7 argumentos posicionais, agora precisa de 8) — corrigido no próximo passo, dentro da mesma task.

- [ ] **Step 2: Write the failing test**

Criar `src/test/java/com/curso/gestaoinvestimentos/service/AcaoServiceTest.java`:

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.curso.gestaoinvestimentos.integration.CotacaoProvider;
import com.curso.gestaoinvestimentos.integration.TwelveDataCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock
    private AcaoRepository repository;
    @Mock
    private HistoricoCotacaoRepository historicoRepository;
    @Mock
    private CotacaoProvider cotacaoProvider;
    @Mock
    private TwelveDataCambioClient cambioClient;

    private AcaoService service;

    @BeforeEach
    void setUp() {
        service = new AcaoService(repository, historicoRepository, List.of(cotacaoProvider), cambioClient);
    }

    private Acao acaoEua(String ticker, String cotacao) {
        Acao acao = new Acao();
        acao.setId(1L);
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.EUA);
        acao.setMoeda("USD");
        acao.setCotacaoAtual(new BigDecimal(cotacao));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private Acao acaoBrasil(String ticker) {
        Acao acao = new Acao();
        acao.setId(2L);
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Empresa " + ticker);
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("30.00"));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    @Test
    void listarConverteCotacaoDeAcaoEuaParaReais() {
        when(repository.findAll()).thenReturn(List.of(acaoEua("AAPL", "310.49")));
        when(cambioClient.buscarTaxaUsdParaBrl()).thenReturn(new BigDecimal("5.21"));

        List<AcaoResponseDTO> resultado = service.listar();

        // 310.49 * 5.21 = 1617.6529, arredondado pra 1617.65
        assertEquals(0, resultado.get(0).cotacaoAtualBRL().compareTo(new BigDecimal("1617.65")));
    }

    @Test
    void listarNaoConverteCotacaoDeAcaoBrasil() {
        when(repository.findAll()).thenReturn(List.of(acaoBrasil("PETR4")));

        List<AcaoResponseDTO> resultado = service.listar();

        assertNull(resultado.get(0).cotacaoAtualBRL());
    }

    @Test
    void listarDegradaGraciosamenteQuandoCambioFalha() {
        when(repository.findAll()).thenReturn(List.of(acaoEua("AAPL", "310.49")));
        when(cambioClient.buscarTaxaUsdParaBrl()).thenThrow(new ServicoExternoIndisponivelException("indisponivel"));

        List<AcaoResponseDTO> resultado = service.listar();

        assertNull(resultado.get(0).cotacaoAtualBRL());
        assertEquals(0, resultado.get(0).cotacaoAtual().compareTo(new BigDecimal("310.49")));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\mvnw.cmd test "-Dtest=AcaoServiceTest"`
Expected: FAIL to compile — `AcaoService` ainda não tem um construtor de 4 argumentos (`TwelveDataCambioClient` não existe como dependência ainda) e `AcaoResponseDTO` já tem 8 campos mas `AcaoService.toResponseDTO` só passa 7.

- [ ] **Step 4: Update `AcaoService`**

Substituir o arquivo inteiro `src/main/java/com/curso/gestaoinvestimentos/service/AcaoService.java` por:

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.AcaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.AcaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.HistoricoCotacaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoDuplicadoException;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.ServicoExternoIndisponivelException;
import com.curso.gestaoinvestimentos.integration.CotacaoProvider;
import com.curso.gestaoinvestimentos.integration.DadosCotacaoResponse;
import com.curso.gestaoinvestimentos.integration.TwelveDataCambioClient;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.HistoricoCotacao;
import com.curso.gestaoinvestimentos.model.Mercado;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.HistoricoCotacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AcaoService {

    private final AcaoRepository repository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final List<CotacaoProvider> providers;
    private final TwelveDataCambioClient cambioClient;

    public AcaoService(AcaoRepository repository, HistoricoCotacaoRepository historicoRepository,
                        List<CotacaoProvider> providers, TwelveDataCambioClient cambioClient) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.providers = providers;
        this.cambioClient = cambioClient;
    }

    public AcaoResponseDTO criar(AcaoRequestDTO dto) {
        repository.findByTicker(dto.ticker()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe uma acao cadastrada com o ticker " + dto.ticker());
        });

        DadosCotacaoResponse dadosCotacao = buscarCotacao(dto.ticker(), dto.mercado());

        Acao acao = new Acao();
        acao.setTicker(dadosCotacao.ticker());
        acao.setNomeEmpresa(dadosCotacao.nomeEmpresa());
        acao.setMercado(dto.mercado());
        acao.setMoeda(dadosCotacao.moeda());
        acao.setCotacaoAtual(dadosCotacao.cotacaoAtual());
        acao.setDataHoraCotacao(dadosCotacao.dataHoraCotacao());

        Acao salva = repository.save(acao);
        registrarHistorico(salva, dadosCotacao);
        return toResponseDTO(salva);
    }

    public List<AcaoResponseDTO> listar() {
        List<Acao> acoes = repository.findAll();
        boolean temAcaoEua = acoes.stream().anyMatch(a -> a.getMercado() == Mercado.EUA);
        BigDecimal taxaCambio = temAcaoEua ? buscarTaxaCambioSeguro() : null;
        return acoes.stream()
                .map(a -> toResponseDTO(a, taxaCambio))
                .toList();
    }

    public AcaoResponseDTO buscarPorId(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + id));
        return toResponseDTO(acao);
    }

    public AcaoResponseDTO buscarPorTicker(String ticker) {
        Acao acao = repository.findByTicker(ticker)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com ticker " + ticker));
        return toResponseDTO(acao);
    }

    public AcaoResponseDTO atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + id));

        DadosCotacaoResponse dadosCotacao = buscarCotacao(acao.getTicker(), acao.getMercado());
        acao.setCotacaoAtual(dadosCotacao.cotacaoAtual());
        acao.setDataHoraCotacao(dadosCotacao.dataHoraCotacao());

        Acao salva = repository.save(acao);
        registrarHistorico(salva, dadosCotacao);
        return toResponseDTO(salva);
    }

    public List<HistoricoCotacaoResponseDTO> historico(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Acao nao encontrada com id " + id);
        }
        return historicoRepository.findByAcaoIdOrderByCapturadoEmDesc(id).stream()
                .map(h -> new HistoricoCotacaoResponseDTO(h.getPreco(), h.getCapturadoEm()))
                .toList();
    }

    private void registrarHistorico(Acao acao, DadosCotacaoResponse dadosCotacao) {
        HistoricoCotacao historico = new HistoricoCotacao();
        historico.setAcao(acao);
        historico.setPreco(dadosCotacao.cotacaoAtual());
        historico.setCapturadoEm(dadosCotacao.dataHoraCotacao());
        historicoRepository.save(historico);
    }

    /**
     * Selecao do Strategy: percorre os providers disponiveis e usa o primeiro
     * que declarar suporte ao mercado pedido. Adicionar um mercado novo no
     * futuro significa criar um CotacaoProvider novo, sem alterar este metodo.
     */
    private DadosCotacaoResponse buscarCotacao(String ticker, Mercado mercado) {
        CotacaoProvider provider = providers.stream()
                .filter(p -> p.suporta(mercado))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum provedor de cotacao disponivel para o mercado " + mercado));

        return provider.buscarCotacao(ticker);
    }

    /**
     * Conversao pra reais e so um dado de exibicao complementar -- se a
     * Twelve Data estiver fora do ar, a acao (e a cotacao original) continuam
     * sendo retornadas normalmente, so sem o valor convertido.
     */
    private BigDecimal buscarTaxaCambioSeguro() {
        try {
            return cambioClient.buscarTaxaUsdParaBrl();
        } catch (ServicoExternoIndisponivelException ex) {
            return null;
        }
    }

    private AcaoResponseDTO toResponseDTO(Acao acao) {
        BigDecimal taxaCambio = acao.getMercado() == Mercado.EUA ? buscarTaxaCambioSeguro() : null;
        return toResponseDTO(acao, taxaCambio);
    }

    private AcaoResponseDTO toResponseDTO(Acao acao, BigDecimal taxaCambio) {
        BigDecimal cotacaoAtualBRL = null;
        if (acao.getMercado() == Mercado.EUA && acao.getCotacaoAtual() != null && taxaCambio != null) {
            cotacaoAtualBRL = acao.getCotacaoAtual().multiply(taxaCambio).setScale(2, RoundingMode.HALF_UP);
        }
        return new AcaoResponseDTO(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                acao.getCotacaoAtual(),
                cotacaoAtualBRL,
                acao.getDataHoraCotacao()
        );
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\mvnw.cmd test "-Dtest=AcaoServiceTest"`
Expected: PASS — 3 testes verdes.

- [ ] **Step 6: Run the full suite to confirm nothing else broke**

Run: `.\mvnw.cmd test`
Expected: BUILD SUCCESS. Nenhum outro arquivo constrói `AcaoResponseDTO` ou `AcaoService` diretamente, então nada além do que já foi tocado deveria quebrar.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/dto/AcaoResponseDTO.java src/main/java/com/curso/gestaoinvestimentos/service/AcaoService.java src/test/java/com/curso/gestaoinvestimentos/service/AcaoServiceTest.java
git commit -m "feat(acoes): expoe cotacao convertida em reais para acoes EUA"
```

---

### Task 6: Helpers de conversão no frontend + CSS

**Files:**
- Modify: `src/main/resources/static/js/dashboard.js`
- Modify: `src/main/resources/static/css/dashboard.css`

- [ ] **Step 1: Add the JS helpers**

Em `src/main/resources/static/js/dashboard.js`, logo depois da função `fmtResultado` (que termina assim):

```javascript
function fmtResultado(valor, moeda) {
    if (valor == null) return '<span class="pl pl-zero">—</span>';
    const n = Number(valor);
    if (n === 0) return `<span class="pl pl-zero">${esc(fmtMoeda(0, moeda))}</span>`;
    const cls = n > 0 ? "pl-pos" : "pl-neg";
    const sinal = n > 0 ? "+" : "-";
    return `<span class="pl ${cls}">${sinal}${esc(fmtMoeda(Math.abs(n), moeda))}</span>`;
}
```

Adicionar logo em seguida:

```javascript
/**
 * Deriva a taxa de cambio implicita do campo ja convertido que a API manda
 * pra acoes EUA (cotacaoAtualBRL) -- evita um endpoint dedicado de cambio,
 * a mesma taxa serve pra converter qualquer outro valor daquela acao
 * (preco medio, valor investido, valor de uma operacao) no frontend.
 */
function taxaCambioPorTicker(ticker) {
    const a = state.acoes.find((x) => x.ticker === ticker);
    if (!a || a.moeda !== "USD" || a.cotacaoAtualBRL == null || !a.cotacaoAtual) return null;
    return Number(a.cotacaoAtualBRL) / Number(a.cotacaoAtual);
}

function fmtConvertido(valorNaMoedaOriginal, ticker) {
    const taxa = taxaCambioPorTicker(ticker);
    if (taxa == null || valorNaMoedaOriginal == null) return "";
    return `<span class="valor-convertido">≈ ${esc(fmtMoeda(Number(valorNaMoedaOriginal) * taxa, "BRL"))}</span>`;
}
```

- [ ] **Step 2: Add the CSS rule**

Em `src/main/resources/static/css/dashboard.css`, logo depois da regra `.pl-zero` (atualmente):

```css
.pl-zero { color: var(--rendo-color-text-muted); }
```

Adicionar logo em seguida:

```css
.valor-convertido { display: block; font-size: 11.5px; font-weight: 400; color: var(--rendo-color-text-muted); margin-top: 2px; }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/dashboard.js src/main/resources/static/css/dashboard.css
git commit -m "feat(frontend): adiciona helpers de conversao para reais"
```

Nenhum teste automatizado pra este passo isolado — os helpers só são exercitados de verdade quando aplicados nas telas (Task 7), verificado manualmente no navegador (Task 9).

---

### Task 7: Aplicar a conversão nas telas

**Files:**
- Modify: `src/main/resources/static/js/dashboard.js`

- [ ] **Step 1: Catálogo de Ações — cotação atual**

Em `renderAcoes()`, atualmente:

```javascript
    tbody.innerHTML = lista.map((a) => `
        <tr>
            <td>${esc(a.ticker)}</td>
            <td>${esc(a.nomeEmpresa || "—")}</td>
            <td>${mercadoTag(a.mercado)}</td>
            <td class="num">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</td>
            <td class="acoes-col">
```

Substituir a linha da cotação por (a taxa já vem pronta em `a.cotacaoAtualBRL`, não precisa de `fmtConvertido` aqui):

```javascript
    tbody.innerHTML = lista.map((a) => `
        <tr>
            <td>${esc(a.ticker)}</td>
            <td>${esc(a.nomeEmpresa || "—")}</td>
            <td>${mercadoTag(a.mercado)}</td>
            <td class="num">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}${a.cotacaoAtualBRL != null ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(a.cotacaoAtualBRL, "BRL"))}</span>` : ""}</td>
            <td class="acoes-col">
```

- [ ] **Step 2: Painel de detalhes de uma ação**

Em `detalheAcaoHTML(a)`, atualmente:

```javascript
            <div class="detail-item"><span class="k">Cotação atual</span><span class="v">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</span></div>
```

Substituir por:

```javascript
            <div class="detail-item"><span class="k">Cotação atual</span><span class="v">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}${a.cotacaoAtualBRL != null ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(a.cotacaoAtualBRL, "BRL"))}</span>` : ""}</span></div>
```

- [ ] **Step 3: Visão Geral — mini-lista de posições e "Não realizado"**

Em `renderVisaoGeral()`, atualmente:

```javascript
    let naoRealizado = 0;
    for (const p of state.posicoes) {
        if (p.valorAtual != null && p.valorInvestido != null) naoRealizado += Number(p.valorAtual) - Number(p.valorInvestido);
    }
```

Substituir por (converte cada posição EUA pra reais antes de somar — hoje soma os números brutos misturados, rotulando o total como "BRL" mesmo quando parte dele é USD):

```javascript
    let naoRealizado = 0;
    for (const p of state.posicoes) {
        if (p.valorAtual == null || p.valorInvestido == null) continue;
        const taxa = taxaCambioPorTicker(p.acaoTicker) ?? 1;
        naoRealizado += (Number(p.valorAtual) - Number(p.valorInvestido)) * taxa;
    }
```

Mais abaixo, na mesma função, atualmente:

```javascript
    dash.innerHTML = recentes.map((p) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        return `
        <div class="mini-row">
            <span class="m-ticker">${esc(p.acaoTicker)}</span>
            <span class="m-corretora">${esc(p.corretoraNome)}</span>
            <span class="m-qtd">${esc(fmtNumero(p.quantidade))} un.</span>
            <span class="m-preco-medio">PM ${esc(fmtMoeda(p.precoMedio, moeda))}</span>
            <span class="m-valor">${esc(fmtMoeda(p.valorAtual, moeda))}</span>
        </div>`;
    }).join("");
```

Substituir por:

```javascript
    dash.innerHTML = recentes.map((p) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        return `
        <div class="mini-row">
            <span class="m-ticker">${esc(p.acaoTicker)}</span>
            <span class="m-corretora">${esc(p.corretoraNome)}</span>
            <span class="m-qtd">${esc(fmtNumero(p.quantidade))} un.</span>
            <span class="m-preco-medio">PM ${esc(fmtMoeda(p.precoMedio, moeda))}</span>
            <span class="m-valor">${esc(fmtMoeda(p.valorAtual, moeda))}${fmtConvertido(p.valorAtual, p.acaoTicker)}</span>
        </div>`;
    }).join("");
```

- [ ] **Step 4: Minhas Posições — tabela**

Em `renderPosicoes()`, atualmente:

```javascript
    tbody.innerHTML = lista.map((p, i) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        const resultado = p.valorAtual != null ? Number(p.valorAtual) - Number(p.valorInvestido) : null;
        return `
        <tr>
            <td>${esc(p.acaoTicker)}</td>
            <td>${esc(p.corretoraNome)}</td>
            <td class="num">${esc(fmtNumero(p.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(p.precoMedio, moeda))}</td>
            <td class="num">${esc(fmtMoeda(p.valorInvestido, moeda))}</td>
            <td class="num">${esc(fmtMoeda(p.valorAtual, moeda))}</td>
            <td class="num">${fmtResultado(resultado, moeda)}</td>
            <td class="acoes-col"><button class="btn btn-sell btn-icon" data-sell="${i}">Vender</button></td>
        </tr>`;
    }).join("");
```

Substituir por:

```javascript
    tbody.innerHTML = lista.map((p, i) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        const resultado = p.valorAtual != null ? Number(p.valorAtual) - Number(p.valorInvestido) : null;
        return `
        <tr>
            <td>${esc(p.acaoTicker)}</td>
            <td>${esc(p.corretoraNome)}</td>
            <td class="num">${esc(fmtNumero(p.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(p.precoMedio, moeda))}${fmtConvertido(p.precoMedio, p.acaoTicker)}</td>
            <td class="num">${esc(fmtMoeda(p.valorInvestido, moeda))}${fmtConvertido(p.valorInvestido, p.acaoTicker)}</td>
            <td class="num">${esc(fmtMoeda(p.valorAtual, moeda))}${fmtConvertido(p.valorAtual, p.acaoTicker)}</td>
            <td class="num">${fmtResultado(resultado, moeda)}${fmtConvertido(resultado, p.acaoTicker)}</td>
            <td class="acoes-col"><button class="btn btn-sell btn-icon" data-sell="${i}">Vender</button></td>
        </tr>`;
    }).join("");
```

- [ ] **Step 5: Modal de compra/venda — resumo**

Em `ligarResumoOperacao(acao, posicao)`, atualmente:

```javascript
function ligarResumoOperacao(acao, posicao) {
    const form = $("#formOperacao");
    const moeda = acao.moeda;
    const calcResumo = () => {
        const qtd = Number(form.quantidade.value || 0);
        const preco = Number(form.precoUnitario.value || 0);
        const box = $("#opResumo");
        if (!qtd || qtd <= 0 || !preco) { box.innerHTML = ""; return; }
        const total = qtd * preco;
        let html = `<div class="op-resumo-row"><span>Valor total</span><b>${esc(fmtMoeda(total, moeda))}</b></div>`;
        if (posicao) {
            const resultado = (preco - Number(posicao.precoMedio)) * qtd;
            html += `<div class="op-resumo-row"><span>Resultado estimado</span>${fmtResultado(resultado, moeda)}</div>`;
        }
        box.innerHTML = html;
    };
```

Substituir por (esse é o exato momento do exemplo do usuário: "comprei a US$310,49, mostra que vou pagar ≈R$X"):

```javascript
function ligarResumoOperacao(acao, posicao) {
    const form = $("#formOperacao");
    const moeda = acao.moeda;
    const calcResumo = () => {
        const qtd = Number(form.quantidade.value || 0);
        const preco = Number(form.precoUnitario.value || 0);
        const box = $("#opResumo");
        if (!qtd || qtd <= 0 || !preco) { box.innerHTML = ""; return; }
        const total = qtd * preco;
        let html = `<div class="op-resumo-row"><span>Valor total</span><b>${esc(fmtMoeda(total, moeda))}${fmtConvertido(total, acao.ticker)}</b></div>`;
        if (posicao) {
            const resultado = (preco - Number(posicao.precoMedio)) * qtd;
            html += `<div class="op-resumo-row"><span>Resultado estimado</span>${fmtResultado(resultado, moeda)}${fmtConvertido(resultado, acao.ticker)}</div>`;
        }
        box.innerHTML = html;
    };
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/js/dashboard.js
git commit -m "feat(frontend): mostra o valor convertido em reais nas telas de acoes EUA"
```

---

### Task 8: Rodar a suíte completa

- [ ] **Step 1: Run every test**

Run: `.\mvnw.cmd test`
Expected: BUILD SUCCESS, 0 failures.

---

### Task 9: Verificação manual no navegador

Cobre o cenário exato do pedido original: cadastrar uma ação americana, ver a cotação convertida no catálogo, comprar e ver quanto vai pagar em reais antes de confirmar, e depois ver a posição já com o valor convertido no painel.

- [ ] **Step 1: Start the app**

Run: `.\mvnw.cmd spring-boot:run`

(Se o app já estava rodando de uma sessão anterior, mate o processo antigo primeiro — edições em `static/`/`templates/` não hot-reload em `spring-boot:run` já em execução.)

- [ ] **Step 2: Cadastrar uma ação americana e conferir a cotação convertida**

Login no dashboard, ir em "Ações", cadastrar um ticker americano real (ex.: `AAPL`, mercado EUA). Confirmar: a linha da tabela mostra a cotação em US$ com uma segunda linha discreta "≈ R$X,XX" embaixo. Abrir os detalhes da ação (botão "⤢") e confirmar que a mesma conversão aparece lá.

(Se a chave `TWELVEDATA_API_KEY` configurada for a `demo` padrão, a busca de câmbio pode falhar — nesse caso a cotação em US$ ainda deve aparecer normalmente, só sem a linha "≈ R$". Isso é o comportamento esperado de degradação graciosa, não um bug.)

- [ ] **Step 3: Comprar a ação e ver o valor convertido antes de confirmar**

Clicar em "Comprar", preencher corretora/quantidade/preço. Confirmar que o resumo mostra "Valor total: US$X,XX" com uma linha "≈ R$Y,YY" logo abaixo, atualizando ao vivo conforme muda a quantidade/preço. Confirmar a compra.

- [ ] **Step 4: Conferir o painel de posições**

Ir em "Minhas Posições" e na "Visão Geral". Confirmar que preço médio, valor investido, valor atual e resultado da posição em ação americana mostram a linha "≈ R$" convertida. Confirmar que o card "Não realizado" da Visão Geral é um número plausível em reais (não a soma bruta de US$ com R$ de outras posições, se houver alguma em BRASIL).

- [ ] **Step 5: Conferir que o saldo foi descontado no valor certo**

Anotar o saldo disponível antes da compra (Step 3) e depois. Confirmar que a diferença bate com o valor em reais mostrado no resumo da compra (Step 3), não com o número bruto em dólar multiplicado pela quantidade.

- [ ] **Step 6: Checar o console do navegador**

Sem erros novos no console (só ruído de extensão do Chrome, se houver, como já visto em sessões anteriores).
