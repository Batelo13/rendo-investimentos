# Carteira de Investimentos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `Carteira` domain (portfolio) that connects `Usuario`, `Corretora` and `Acao`: a 1:1 `Carteira` per user (auto-created at registration) holding an immutable history of `Operacao` (buy/sell), with the current position (quantity, weighted-average price) always calculated from that history — never stored.

**Architecture:** Standard layered structure matching the existing codebase (`model` → `repository` → `service` → `controller`, DTOs as records, exceptions via `GlobalExceptionHandler`). The one new piece with real logic is `PosicaoCalculator`, a dependency-free pure-function component that walks a chronologically-ordered list of `Operacao` and returns quantity + weighted-average price — reused both for "current position" and for snapshotting `precoMedioNaVenda` at the moment of each sale. This class gets strict TDD treatment (it's the exact rule that cost points in a prior version of this project — see spec). Everything else follows this codebase's established convention: implement directly per the CRUD pattern already used by `Usuario`/`Acao`/`Corretora`, verify with `mvn compile` as you go, and add MockMvc integration tests (same style as `UsuarioAuthIntegrationTest`) once the endpoints exist end-to-end.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA, Spring Security (session-based), H2 (dev profile, `create-drop`), JUnit 5, MockMvc, Jackson 3 (`tools.jackson.*` — Spring Boot 4 moved off `com.fasterxml.jackson.*`).

**Spec:** `docs/superpowers/specs/2026-08-09-carteira-investimentos-design.md`

**Branch:** `6-carteira` (already checked out)

---

## Task 1: Enums + `Carteira` entity + `CarteiraRepository`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/model/TipoOperacao.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/model/StatusOperacao.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/model/Carteira.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/repository/CarteiraRepository.java`

- [ ] **Step 1: Create `TipoOperacao`**

```java
package com.curso.gestaoinvestimentos.model;

public enum TipoOperacao {
    COMPRA,
    VENDA
}
```

- [ ] **Step 2: Create `StatusOperacao`**

```java
package com.curso.gestaoinvestimentos.model;

public enum StatusOperacao {
    ATIVA,
    CANCELADA
}
```

- [ ] **Step 3: Create the `Carteira` entity**

```java
package com.curso.gestaoinvestimentos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "carteiras")
public class Carteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    private LocalDate dataCriacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDate dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
```

- [ ] **Step 4: Create `CarteiraRepository`**

```java
package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {

    Optional<Carteira> findByUsuarioId(Long usuarioId);
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0 (Maven `-q` is silent on success).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/model/TipoOperacao.java src/main/java/com/curso/gestaoinvestimentos/model/StatusOperacao.java src/main/java/com/curso/gestaoinvestimentos/model/Carteira.java src/main/java/com/curso/gestaoinvestimentos/repository/CarteiraRepository.java
git commit -m "$(cat <<'EOF'
feat(carteira): entidade Carteira e enums de Operacao

Carteira e 1:1 com Usuario. TipoOperacao (COMPRA/VENDA) e
StatusOperacao (ATIVA/CANCELADA) sao usados pela entidade Operacao,
adicionada no proximo commit.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: `Operacao` entity + `OperacaoRepository`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/model/Operacao.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/repository/OperacaoRepository.java`

- [ ] **Step 1: Create the `Operacao` entity**

```java
package com.curso.gestaoinvestimentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operacoes")
public class Operacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOperacao tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    private BigDecimal precoMedioNaVenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOperacao status;

    private LocalDateTime canceladaEm;

    @ManyToOne
    @JoinColumn(name = "cancelada_por_id")
    private Usuario canceladaPor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public void setCarteira(Carteira carteira) {
        this.carteira = carteira;
    }

    public Acao getAcao() {
        return acao;
    }

    public void setAcao(Acao acao) {
        this.acao = acao;
    }

    public Corretora getCorretora() {
        return corretora;
    }

    public void setCorretora(Corretora corretora) {
        this.corretora = corretora;
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperacao tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public BigDecimal getPrecoMedioNaVenda() {
        return precoMedioNaVenda;
    }

    public void setPrecoMedioNaVenda(BigDecimal precoMedioNaVenda) {
        this.precoMedioNaVenda = precoMedioNaVenda;
    }

    public StatusOperacao getStatus() {
        return status;
    }

    public void setStatus(StatusOperacao status) {
        this.status = status;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public void setCanceladaEm(LocalDateTime canceladaEm) {
        this.canceladaEm = canceladaEm;
    }

    public Usuario getCanceladaPor() {
        return canceladaPor;
    }

    public void setCanceladaPor(Usuario canceladaPor) {
        this.canceladaPor = canceladaPor;
    }
}
```

- [ ] **Step 2: Create `OperacaoRepository`**

```java
package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    List<Operacao> findByCarteiraIdOrderByDataHoraDesc(Long carteiraId);

    List<Operacao> findByCarteiraIdAndStatusOrderByDataHoraAsc(Long carteiraId, StatusOperacao status);

    List<Operacao> findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
            Long carteiraId, Long acaoId, Long corretoraId, StatusOperacao status);
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/model/Operacao.java src/main/java/com/curso/gestaoinvestimentos/repository/OperacaoRepository.java
git commit -m "$(cat <<'EOF'
feat(carteira): entidade Operacao (historico de compra/venda)

Registro imutavel de uma operacao de compra ou venda, ligado a
Carteira + Acao + Corretora. precoMedioNaVenda fica congelado no
proprio registro quando tipo=VENDA (preenchido no service, proximo
commit). status permite soft-delete (CANCELADA) por um ADMIN, sem
apagar o registro nem recalcular vendas passadas.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: `PosicaoCalculator` (TDD — this is the business rule that must be perfect)

This is the exact bug from the spec: a naive "sum all buys / current net quantity" formula
incorrectly "inherits" average price from a lot that was already fully sold. Strict TDD here,
with no Spring context needed (pure logic, fast tests).

**Files:**
- Test: `src/test/java/com/curso/gestaoinvestimentos/service/PosicaoCalculatorTest.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/service/PosicaoCalculator.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosicaoCalculatorTest {

    private Operacao operacao(TipoOperacao tipo, int quantidade, String precoUnitario) {
        Operacao operacao = new Operacao();
        operacao.setTipo(tipo);
        operacao.setQuantidade(quantidade);
        operacao.setPrecoUnitario(new BigDecimal(precoUnitario));
        return operacao;
    }

    @Test
    void compraUnicaDefinePrecoMedioIgualAoPrecoDeCompra() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, 10, "100.00")
        ));

        assertEquals(10, resultado.quantidade());
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void duasComprasCalculamMediaPonderada() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, 10, "100.00"),
                operacao(TipoOperacao.COMPRA, 10, "200.00")
        ));

        assertEquals(20, resultado.quantidade());
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void vendaParcialNaoAlteraPrecoMedioDoQueSobrou() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, 10, "100.00"),
                operacao(TipoOperacao.VENDA, 5, "50.00")
        ));

        assertEquals(5, resultado.quantidade());
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void zerarPosicaoEComprarDeNovoReiniciaPrecoMedioDoZero() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, 10, "10.00"),
                operacao(TipoOperacao.VENDA, 10, "10.00"),
                operacao(TipoOperacao.COMPRA, 5, "20.00")
        ));

        assertEquals(5, resultado.quantidade());
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void detectaSaldoNegativoHistoricoQuandoUmaCompraEhRemovidaDaSimulacao() {
        // Sem a compra (simulando um cancelamento), a venda de 5 ficaria a
        // descoberto naquele momento, mesmo que a compra seguinte "equilibre"
        // o total no final -- por isso o minimo historico, nao so o final,
        // e o que importa pra validar um cancelamento.
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.VENDA, 5, "50.00"),
                operacao(TipoOperacao.COMPRA, 10, "20.00")
        ));

        assertEquals(5, resultado.quantidade());
        assertEquals(-5, resultado.quantidadeMinimaHistorica());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw.cmd -q -Dtest=PosicaoCalculatorTest test`
Expected: compilation failure — `PosicaoCalculator` does not exist yet (`cannot find symbol`).

- [ ] **Step 3: Write the implementation**

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula a posicao (quantidade, preco medio) a partir de um historico de
 * operacoes em ordem cronologica. Preco medio so muda em COMPRA (media
 * ponderada); VENDA reduz quantidade sem alterar o preco medio de quem fica
 * na carteira, e reseta o preco medio quando a quantidade zera -- sem isso,
 * uma posicao que foi zerada e recomecada "herdaria" preco medio de um lote
 * que ja nao existe mais.
 */
public class PosicaoCalculator {

    public record Posicao(int quantidade, BigDecimal precoMedio, int quantidadeMinimaHistorica) {
    }

    public static Posicao calcular(List<Operacao> operacoesEmOrdemCronologica) {
        int quantidade = 0;
        BigDecimal precoMedio = BigDecimal.ZERO;
        int quantidadeMinima = 0;

        for (Operacao operacao : operacoesEmOrdemCronologica) {
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                int novaQuantidade = quantidade + operacao.getQuantidade();
                BigDecimal custoAntigo = precoMedio.multiply(BigDecimal.valueOf(quantidade));
                BigDecimal custoNovo = operacao.getPrecoUnitario().multiply(BigDecimal.valueOf(operacao.getQuantidade()));
                precoMedio = custoAntigo.add(custoNovo)
                        .divide(BigDecimal.valueOf(novaQuantidade), 6, RoundingMode.HALF_UP);
                quantidade = novaQuantidade;
            } else {
                quantidade -= operacao.getQuantidade();
                if (quantidade == 0) {
                    precoMedio = BigDecimal.ZERO;
                }
            }
            quantidadeMinima = Math.min(quantidadeMinima, quantidade);
        }

        return new Posicao(quantidade, precoMedio, quantidadeMinima);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw.cmd -q -Dtest=PosicaoCalculatorTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.service.PosicaoCalculatorTest.txt`
Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/PosicaoCalculator.java src/test/java/com/curso/gestaoinvestimentos/service/PosicaoCalculatorTest.java
git commit -m "$(cat <<'EOF'
feat(carteira): PosicaoCalculator com preco medio ponderado correto

Componente puro (sem dependencias, sem contexto Spring) que calcula
quantidade e preco medio a partir do historico de operacoes em ordem
cronologica: preco medio so muda em compra, venda nao altera o preco
medio de quem fica na carteira, e reseta corretamente quando a
posicao zera. Corrige o bug classico de formula ingenua que "herda"
preco medio de um lote ja vendido por completo.

quantidadeMinimaHistorica existe para validar cancelamento de compra:
o minimo ao longo do tempo importa, nao so o total final, porque uma
compra posterior pode "mascarar" um deficit que existiu no meio do
caminho.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: `RegraDeNegocioException` + `GlobalExceptionHandler`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/exception/RegraDeNegocioException.java`
- Modify: `src/main/java/com/curso/gestaoinvestimentos/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create `RegraDeNegocioException`**

```java
package com.curso.gestaoinvestimentos.exception;

public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Add the handler to `GlobalExceptionHandler`**

Modify `src/main/java/com/curso/gestaoinvestimentos/exception/GlobalExceptionHandler.java`: insert
this method right after `handleDuplicado` (which ends at line 36 today):

```java
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraDeNegocio(RegraDeNegocioException ex) {
        ErrorResponse erro = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }
```

No new imports needed — `HttpStatus`, `ResponseEntity` and `LocalDateTime` are already imported
in that file.

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/exception/RegraDeNegocioException.java src/main/java/com/curso/gestaoinvestimentos/exception/GlobalExceptionHandler.java
git commit -m "$(cat <<'EOF'
feat(carteira): excecao de regra de negocio (422)

RegraDeNegocioException cobre violacoes especificas do dominio
Carteira: venda a descoberto, corretora nao validada na CVM,
cancelamento que negativaria o saldo, cancelar operacao que nao e
compra. Segue o mesmo padrao ja usado por RecursoNaoEncontradoException
e RecursoDuplicadoException no GlobalExceptionHandler.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: DTOs

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/dto/OperacaoRequestDTO.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/dto/OperacaoResponseDTO.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/dto/PosicaoDTO.java`

- [ ] **Step 1: Create `OperacaoRequestDTO`**

```java
package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.TipoOperacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OperacaoRequestDTO(

        @NotNull(message = "Acao e obrigatoria")
        Long acaoId,

        @NotNull(message = "Corretora e obrigatoria")
        Long corretoraId,

        @NotNull(message = "Tipo e obrigatorio (COMPRA ou VENDA)")
        TipoOperacao tipo,

        @NotNull(message = "Quantidade e obrigatoria")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Preco unitario e obrigatorio")
        @Positive(message = "Preco unitario deve ser maior que zero")
        BigDecimal precoUnitario
) {
}
```

- [ ] **Step 2: Create `OperacaoResponseDTO`**

```java
package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacaoResponseDTO(
        Long id,
        TipoOperacao tipo,
        Integer quantidade,
        BigDecimal precoUnitario,
        LocalDateTime dataHora,
        StatusOperacao status,
        String acaoTicker,
        String corretoraNome,
        BigDecimal precoMedioNaVenda,
        BigDecimal lucroPrejuizoRealizado
) {
}
```

- [ ] **Step 3: Create `PosicaoDTO`**

```java
package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;

public record PosicaoDTO(
        String acaoTicker,
        String corretoraNome,
        Integer quantidade,
        BigDecimal precoMedio,
        BigDecimal valorInvestido,
        BigDecimal valorAtual
) {
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/dto/OperacaoRequestDTO.java src/main/java/com/curso/gestaoinvestimentos/dto/OperacaoResponseDTO.java src/main/java/com/curso/gestaoinvestimentos/dto/PosicaoDTO.java
git commit -m "$(cat <<'EOF'
feat(carteira): DTOs de Operacao e Posicao

OperacaoRequestDTO nunca aceita carteira/usuario do cliente (sempre
do contexto de autenticacao, no service). OperacaoResponseDTO carrega
precoMedioNaVenda e lucroPrejuizoRealizado (derivado, so preenchido
em vendas). PosicaoDTO e a posicao agregada por acao+corretora.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: `Carteira` automática no cadastro de `Usuario`

Este passo cria uma dependência nova: toda vez que um `Usuario` é criado, uma `Carteira` é
criada junto (FK obrigatória `usuario_id`). Isso quebra a limpeza de banco do teste de
autenticação já existente, que hoje só apaga `usuarios` — precisa apagar `carteiras` primeiro.

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/UsuarioService.java`
- Modify: `src/test/java/com/curso/gestaoinvestimentos/UsuarioAuthIntegrationTest.java`

- [ ] **Step 1: Inject `CarteiraRepository` and create the `Carteira` in `cadastrar()`**

In `UsuarioService.java`, add the import and field, and change the constructor:

```java
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
```

```java
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                           CarteiraRepository carteiraRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.carteiraRepository = carteiraRepository;
    }
```

And add the `Carteira` import plus its creation at the end of `cadastrar()`, right before the
`return`:

```java
import com.curso.gestaoinvestimentos.model.Carteira;
```

```java
        Usuario salvo = repository.save(usuario);

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteiraRepository.save(carteira);

        return toResponseDTO(salvo);
```

- [ ] **Step 2: Fix `UsuarioAuthIntegrationTest` cleanup for the new FK**

Add the import and field, and update `limparBanco()`:

```java
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
```

```java
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CarteiraRepository carteiraRepository;

    // H2 em memoria e recriado uma vez por contexto Spring, nao por teste -
    // sem isso, cadastros de um teste vazam pro proximo (ex: email duplicado).
    // Carteira e criada automaticamente no cadastro (FK obrigatoria pra
    // usuarios), por isso precisa ser apagada antes.
    @AfterEach
    void limparBanco() {
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
```

- [ ] **Step 3: Run the existing auth test suite to confirm nothing broke**

Run: `./mvnw.cmd -q -Dtest=UsuarioAuthIntegrationTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.UsuarioAuthIntegrationTest.txt`
Expected: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/UsuarioService.java src/test/java/com/curso/gestaoinvestimentos/UsuarioAuthIntegrationTest.java
git commit -m "$(cat <<'EOF'
feat(carteira): cria Carteira automaticamente no cadastro de Usuario

Mesma transacao de UsuarioService.cadastrar -- usuario nunca fica sem
carteira, sem endpoint de criacao separado. Ajusta a limpeza do teste
de autenticacao existente pra apagar Carteira antes de Usuario (FK).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: `OperacaoService`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java`

- [ ] **Step 1: Create `OperacaoService`**

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.exception.RegraDeNegocioException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OperacaoService {

    private final OperacaoRepository operacaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final UsuarioRepository usuarioRepository;

    public OperacaoService(OperacaoRepository operacaoRepository, CarteiraRepository carteiraRepository,
                            AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            UsuarioRepository usuarioRepository) {
        this.operacaoRepository = operacaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public OperacaoResponseDTO registrar(String emailUsuarioAutenticado, OperacaoRequestDTO dto) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));

        Acao acao = acaoRepository.findById(dto.acaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao nao encontrada com id " + dto.acaoId()));
        Corretora corretora = corretoraRepository.findById(dto.corretoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora nao encontrada com id " + dto.corretoraId()));

        if (!Boolean.TRUE.equals(corretora.getValidadaNaCvm())) {
            throw new RegraDeNegocioException("Corretora " + corretora.getNomeFantasia() + " nao e validada na CVM");
        }

        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                carteira.getId(), acao.getId(), corretora.getId(), StatusOperacao.ATIVA);
        PosicaoCalculator.Posicao posicaoAntes = PosicaoCalculator.calcular(historico);

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setCorretora(corretora);
        operacao.setTipo(dto.tipo());
        operacao.setQuantidade(dto.quantidade());
        operacao.setPrecoUnitario(dto.precoUnitario());
        operacao.setDataHora(LocalDateTime.now());
        operacao.setStatus(StatusOperacao.ATIVA);

        if (dto.tipo() == TipoOperacao.VENDA) {
            if (dto.quantidade() > posicaoAntes.quantidade()) {
                throw new RegraDeNegocioException(
                        "Saldo insuficiente: ha " + posicaoAntes.quantidade() + " unidade(s) de "
                                + acao.getTicker() + " nessa corretora, tentando vender " + dto.quantidade());
            }
            operacao.setPrecoMedioNaVenda(posicaoAntes.precoMedio());
        }

        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    public List<OperacaoResponseDTO> listarProprias(String emailUsuarioAutenticado) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuario.getId()));
        return listarPorCarteira(carteira);
    }

    public List<OperacaoResponseDTO> listarComoAdmin(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));
        return listarPorCarteira(carteira);
    }

    public OperacaoResponseDTO cancelar(Long operacaoId, String emailAdmin) {
        Usuario admin = buscarUsuarioPorEmail(emailAdmin);

        Operacao operacao = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operacao nao encontrada com id " + operacaoId));

        if (operacao.getTipo() != TipoOperacao.COMPRA) {
            throw new RegraDeNegocioException("Apenas operacoes de COMPRA podem ser canceladas");
        }
        if (operacao.getStatus() == StatusOperacao.CANCELADA) {
            throw new RegraDeNegocioException("Operacao " + operacaoId + " ja esta cancelada");
        }

        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                operacao.getCarteira().getId(), operacao.getAcao().getId(), operacao.getCorretora().getId(), StatusOperacao.ATIVA);

        List<Operacao> historicoSemEssaCompra = new ArrayList<>(historico);
        historicoSemEssaCompra.removeIf(op -> op.getId().equals(operacao.getId()));

        PosicaoCalculator.Posicao simulacao = PosicaoCalculator.calcular(historicoSemEssaCompra);
        if (simulacao.quantidadeMinimaHistorica() < 0) {
            throw new RegraDeNegocioException(
                    "Cancelar essa compra deixaria o saldo negativo em alguma venda posterior");
        }

        operacao.setStatus(StatusOperacao.CANCELADA);
        operacao.setCanceladaEm(LocalDateTime.now());
        operacao.setCanceladaPor(admin);

        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    private List<OperacaoResponseDTO> listarPorCarteira(Carteira carteira) {
        return operacaoRepository.findByCarteiraIdOrderByDataHoraDesc(carteira.getId()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + email));
    }

    private OperacaoResponseDTO toResponseDTO(Operacao operacao) {
        BigDecimal lucroPrejuizoRealizado = null;
        if (operacao.getTipo() == TipoOperacao.VENDA && operacao.getPrecoMedioNaVenda() != null) {
            lucroPrejuizoRealizado = operacao.getPrecoUnitario()
                    .subtract(operacao.getPrecoMedioNaVenda())
                    .multiply(BigDecimal.valueOf(operacao.getQuantidade()));
        }

        return new OperacaoResponseDTO(
                operacao.getId(),
                operacao.getTipo(),
                operacao.getQuantidade(),
                operacao.getPrecoUnitario(),
                operacao.getDataHora(),
                operacao.getStatus(),
                operacao.getAcao().getTicker(),
                operacao.getCorretora().getNomeFantasia(),
                operacao.getPrecoMedioNaVenda(),
                lucroPrejuizoRealizado
        );
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java
git commit -m "$(cat <<'EOF'
feat(carteira): OperacaoService - registrar, listar, cancelar

registrar(): valida corretora validada na CVM e saldo suficiente pra
venda; se VENDA, grava precoMedioNaVenda calculado pela posicao ANTES
dessa venda (via PosicaoCalculator). cancelar(): so aceita COMPRA,
bloqueia se deixaria saldo negativo em alguma venda posterior
(simulacao com PosicaoCalculator excluindo a compra), soft-delete via
status=CANCELADA -- nunca recalcula precoMedioNaVenda ja gravado em
vendas passadas. Identidade do usuario sempre vem do email
autenticado, nunca de um id no corpo da requisicao.

Ainda sem controller -- exercitado pelos testes de integracao numa
tarefa futura.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: `CarteiraService`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/service/CarteiraService.java`

- [ ] **Step 1: Create `CarteiraService`**

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final OperacaoRepository operacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarteiraService(CarteiraRepository carteiraRepository, OperacaoRepository operacaoRepository,
                            UsuarioRepository usuarioRepository) {
        this.carteiraRepository = carteiraRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PosicaoDTO> buscarPosicaoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        return buscarPosicaoPorUsuarioId(usuario.getId());
    }

    public List<PosicaoDTO> buscarPosicaoPorUsuarioId(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));

        List<Operacao> ativas = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteira.getId(), StatusOperacao.ATIVA);

        Map<String, List<Operacao>> porAcaoECorretora = new LinkedHashMap<>();
        for (Operacao operacao : ativas) {
            String chave = operacao.getAcao().getId() + "-" + operacao.getCorretora().getId();
            porAcaoECorretora.computeIfAbsent(chave, k -> new ArrayList<>()).add(operacao);
        }

        List<PosicaoDTO> posicoes = new ArrayList<>();
        for (List<Operacao> grupo : porAcaoECorretora.values()) {
            PosicaoCalculator.Posicao calculada = PosicaoCalculator.calcular(grupo);
            if (calculada.quantidade() <= 0) {
                continue;
            }
            Acao acao = grupo.get(0).getAcao();
            Corretora corretora = grupo.get(0).getCorretora();
            BigDecimal valorInvestido = calculada.precoMedio().multiply(BigDecimal.valueOf(calculada.quantidade()));
            BigDecimal valorAtual = acao.getCotacaoAtual() == null
                    ? null
                    : acao.getCotacaoAtual().multiply(BigDecimal.valueOf(calculada.quantidade()));

            posicoes.add(new PosicaoDTO(
                    acao.getTicker(),
                    corretora.getNomeFantasia(),
                    calculada.quantidade(),
                    calculada.precoMedio(),
                    valorInvestido,
                    valorAtual
            ));
        }
        return posicoes;
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/CarteiraService.java
git commit -m "$(cat <<'EOF'
feat(carteira): CarteiraService - posicao atual agregada

Agrupa as operacoes ATIVAs por acao+corretora (mantendo a ordem
cronologica de cada grupo) e roda o PosicaoCalculator em cada um.
Grupos com quantidade zerada nao aparecem na posicao atual. valorAtual
reaproveita Acao.cotacaoAtual (cotacao "ao vivo" ja mantida pelo
dominio Acao via Strategy).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Controllers

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/controller/OperacaoController.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/controller/CarteiraController.java`

- [ ] **Step 1: Create `OperacaoController`**

```java
package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.service.OperacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/operacoes")
public class OperacaoController {

    private final OperacaoService service;

    public OperacaoController(OperacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OperacaoResponseDTO> registrar(Principal principal, @Valid @RequestBody OperacaoRequestDTO dto) {
        OperacaoResponseDTO criada = service.registrar(principal.getName(), dto);
        URI location = URI.create("/operacoes/" + criada.id());
        return ResponseEntity.created(location).body(criada);
    }

    @PatchMapping("/{id}/cancelar")
    public OperacaoResponseDTO cancelar(Principal principal, @PathVariable Long id) {
        return service.cancelar(id, principal.getName());
    }
}
```

- [ ] **Step 2: Create `CarteiraController`**

```java
package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.service.CarteiraService;
import com.curso.gestaoinvestimentos.service.OperacaoService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/carteiras")
public class CarteiraController {

    private final CarteiraService carteiraService;
    private final OperacaoService operacaoService;

    public CarteiraController(CarteiraService carteiraService, OperacaoService operacaoService) {
        this.carteiraService = carteiraService;
        this.operacaoService = operacaoService;
    }

    @GetMapping("/me")
    public List<PosicaoDTO> posicaoPropria(Principal principal) {
        return carteiraService.buscarPosicaoPropria(principal.getName());
    }

    @GetMapping("/me/operacoes")
    public List<OperacaoResponseDTO> operacoesProprias(Principal principal) {
        return operacaoService.listarProprias(principal.getName());
    }

    @GetMapping("/{usuarioId}")
    public List<PosicaoDTO> posicaoPorUsuario(@PathVariable Long usuarioId) {
        return carteiraService.buscarPosicaoPorUsuarioId(usuarioId);
    }

    @GetMapping("/{usuarioId}/operacoes")
    public List<OperacaoResponseDTO> operacoesPorUsuario(@PathVariable Long usuarioId) {
        return operacaoService.listarComoAdmin(usuarioId);
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/controller/OperacaoController.java src/main/java/com/curso/gestaoinvestimentos/controller/CarteiraController.java
git commit -m "$(cat <<'EOF'
feat(carteira): OperacaoController e CarteiraController

POST /operacoes, PATCH /operacoes/{id}/cancelar, GET /carteiras/me,
GET /carteiras/me/operacoes, GET /carteiras/{usuarioId}, GET
/carteiras/{usuarioId}/operacoes. Identidade sempre via
Principal.getName() (email autenticado) -- nunca um id vindo do
corpo da requisicao. Restricao de role (ADMIN nas rotas de outro
usuario e no cancelamento) fica no SecurityConfig, proximo commit.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: `SecurityConfig`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/security/SecurityConfig.java`

- [ ] **Step 1: Add the new matchers**

The matcher for `/carteiras/me` and `/carteiras/me/operacoes` **must** come before the
`/carteiras/*` ADMIN-only matcher — Ant-style `/carteiras/*` matches a single path segment,
including the literal `me`, so without this ordering `/carteiras/me` would require ADMIN by
accident. Spring Security evaluates matchers in declaration order, first match wins.

Replace the `authorizeHttpRequests` block:

```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
```

with:

```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/carteiras/me", "/carteiras/me/operacoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/carteiras/*", "/carteiras/*/operacoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/operacoes/*/cancelar").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
```

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/security/SecurityConfig.java
git commit -m "$(cat <<'EOF'
feat(carteira): autorizacao das rotas de carteira e operacao

/carteiras/me e /carteiras/me/operacoes: qualquer autenticado.
/carteiras/{usuarioId} e /carteiras/{usuarioId}/operacoes: so ADMIN
(leitura da carteira de outro usuario). PATCH
/operacoes/{id}/cancelar: so ADMIN. Ordem dos matchers importa -- "me"
precisa ser carve-out antes do wildcard generico, senao seria
capturado pela regra de ADMIN por engano.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: Integration tests — compra, venda e preço médio na venda

**Files:**
- Create: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

This mirrors `UsuarioAuthIntegrationTest`'s style (`@SpringBootTest` + `@AutoConfigureMockMvc`,
`MockHttpSession` captured from a real `/login`). `Acao` and `Corretora` fixtures are inserted
directly via their repositories — **not** through `POST /acoes` or `POST /corretoras`, which
call real external APIs (cotação/CNPJ) and would make the test slow and flaky.

- [ ] **Step 1: Create the test file with the first two scenarios**

```java
package com.curso.gestaoinvestimentos;

import com.curso.gestaoinvestimentos.dto.OperacaoRequestDTO;
import com.curso.gestaoinvestimentos.dto.OperacaoResponseDTO;
import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperacaoIntegrationTest {

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

    @AfterEach
    void limparBanco() {
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
        carteiraRepository.save(carteira);

        return salvo;
    }

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

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("100.00"));

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
        assertEquals(10, posicoes[0].quantidade());
        assertEquals(0, posicoes[0].precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void deveGravarPrecoMedioNaVendaEBloquearVendaADescoberto() throws Exception {
        cadastrarUsuario("vendedor@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("PETR4");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("vendedor@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("100.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO vendaADescoberto = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, 15, new BigDecimal("50.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendaADescoberto)))
                .andExpect(status().isUnprocessableEntity());

        OperacaoRequestDTO vendaParcial = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, 5, new BigDecimal("50.00"));
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
}
```

- [ ] **Step 2: Run the tests**

Run: `./mvnw.cmd -q -Dtest=OperacaoIntegrationTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.OperacaoIntegrationTest.txt`
Expected: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

If a `precoMedio`/`precoMedioNaVenda`/`lucroPrejuizoRealizado` assertion fails only on decimal
formatting (not on the underlying value), that's a serialization quirk, not a logic bug — the
`compareTo(...)  == 0` pattern already used here is deliberately scale-insensitive for this
reason, so a real failure here means the calculation itself is wrong and needs investigation,
not the assertion style.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "$(cat <<'EOF'
test(carteira): compra reflete na posicao; venda grava preco medio

Cobre ponta a ponta (MockMvc + Spring Security real) os dois cenarios
centrais: compra aparece corretamente em GET /carteiras/me, e uma
venda grava precoMedioNaVenda e lucroPrejuizoRealizado corretos, alem
de bloquear venda maior que o saldo (422).

Acao e Corretora sao inseridas direto via repository nos testes, nao
via POST /acoes ou /corretoras -- esses endpoints chamam APIs
externas reais (cotacao, CNPJ) e deixariam o teste lento e flaky.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: Integration tests — corretora não validada, autorização e cancelamento

**Files:**
- Modify: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

- [ ] **Step 1: Add the remaining scenarios**

Add these imports:

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
```

Add these test methods to the class (after `deveGravarPrecoMedioNaVendaEBloquearVendaADescoberto`):

```java
    @Test
    void deveBloquearOperacaoEmCorretoraNaoValidadaNaCvm() throws Exception {
        cadastrarUsuario("naovalidado@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("VALE3");
        Corretora corretora = cadastrarCorretora(false);
        MockHttpSession sessao = logar("naovalidado@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("100.00"));

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

        mockMvc.perform(get("/carteiras/" + usuarioComum.getId()).session(sessaoAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void adminCancelaCompraUsuarioComumNaoConsegue() throws Exception {
        cadastrarUsuario("dono@example.com", "senha1234", Role.USER);
        cadastrarUsuario("admin2@example.com", "senha1234", Role.ADMIN);
        Acao acao = cadastrarAcao("ITUB4");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessaoDono = logar("dono@example.com", "senha1234");
        MockHttpSession sessaoAdmin = logar("admin2@example.com", "senha1234");

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("30.00"));
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

        OperacaoRequestDTO compra = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("30.00"));
        MvcResult resultadoCompra = mockMvc.perform(post("/operacoes")
                        .session(sessaoDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra)))
                .andExpect(status().isCreated())
                .andReturn();
        Long operacaoId = objectMapper.readValue(resultadoCompra.getResponse().getContentAsString(), OperacaoResponseDTO.class).id();

        OperacaoRequestDTO venda = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, 8, new BigDecimal("40.00"));
        mockMvc.perform(post("/operacoes")
                        .session(sessaoDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venda)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/operacoes/" + operacaoId + "/cancelar").session(sessaoAdmin))
                .andExpect(status().isUnprocessableEntity());
    }
```

- [ ] **Step 2: Run the tests**

Run: `./mvnw.cmd -q -Dtest=OperacaoIntegrationTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.OperacaoIntegrationTest.txt`
Expected: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "$(cat <<'EOF'
test(carteira): corretora nao validada, autorizacao e cancelamento

Cobre: bloqueio de operacao em corretora nao validada na CVM (422);
isolamento entre usuarios (403 tentando ver carteira alheia); ADMIN
consegue ver carteira de qualquer usuario mas nao consegue nada alem
disso (usuario comum tentando cancelar leva 403, ADMIN consegue);
cancelamento de compra bloqueado quando deixaria saldo negativo numa
venda ja registrada.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: Integration test — o bug original (preço médio "herdado") + suíte completa

**Files:**
- Modify: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

Este é o cenário exato que custou um ponto na versão anterior do projeto: zera a posição
vendendo tudo, compra de novo a um preço diferente, o preço médio tem que recomeçar do zero —
não misturar com o lote anterior. Já está coberto no nível de unidade (Task 3), aqui é a
confirmação ponta a ponta, pela API de verdade.

- [ ] **Step 1: Add the test**

```java
    @Test
    void zerarPosicaoEComprarDeNovoReiniciaPrecoMedio() throws Exception {
        cadastrarUsuario("recomeco@example.com", "senha1234", Role.USER);
        Acao acao = cadastrarAcao("WEGE3");
        Corretora corretora = cadastrarCorretora(true);
        MockHttpSession sessao = logar("recomeco@example.com", "senha1234");

        OperacaoRequestDTO compra1 = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 10, new BigDecimal("10.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra1)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO vendeTudo = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.VENDA, 10, new BigDecimal("10.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendeTudo)))
                .andExpect(status().isCreated());

        OperacaoRequestDTO compra2 = new OperacaoRequestDTO(acao.getId(), corretora.getId(), TipoOperacao.COMPRA, 5, new BigDecimal("20.00"));
        mockMvc.perform(post("/operacoes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra2)))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/carteiras/me").session(sessao))
                .andExpect(status().isOk())
                .andReturn();
        PosicaoDTO[] posicoes = objectMapper.readValue(resultado.getResponse().getContentAsString(), PosicaoDTO[].class);

        assertEquals(1, posicoes.length);
        assertEquals(5, posicoes[0].quantidade());
        assertEquals(0, posicoes[0].precoMedio().compareTo(new BigDecimal("20.00")));
    }
```

- [ ] **Step 2: Run this test class**

Run: `./mvnw.cmd -q -Dtest=OperacaoIntegrationTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.OperacaoIntegrationTest.txt`
Expected: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 3: Run the full project test suite**

Run: `./mvnw.cmd -q test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/*.txt | grep -E "Tests run|Test set"`
Expected:
```
Test set: com.curso.gestaoinvestimentos.GestaoInvestimentosApplicationTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.OperacaoIntegrationTest
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.UsuarioAuthIntegrationTest
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.service.PosicaoCalculatorTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, ...
```

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "$(cat <<'EOF'
test(carteira): cenario ponta a ponta do bug de preco medio herdado

Zera a posicao vendendo tudo, compra de novo a um preco diferente:
GET /carteiras/me confirma que o preco medio recomeca do zero, sem
misturar com o lote ja vendido por completo. Mesma regra ja coberta
em unidade (PosicaoCalculatorTest), agora validada ponta a ponta pela
API real -- e o bug exato que custou um ponto na versao anterior
deste projeto academico.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage:**
- `Carteira` 1:1, criação automática → Task 1, Task 6.
- `Operacao` com todos os campos do spec (incl. `precoMedioNaVenda`, `status`, `canceladaEm`,
  `canceladaPor`) → Task 2.
- Cálculo de posição com reset no zero (o bug da versão anterior) → Task 3, validado em
  unidade e ponta a ponta (Task 13).
- `precoMedioNaVenda` congelado no momento da venda, nunca recalculado depois → Task 7
  (`registrar`), validado na Task 11.
- Bloqueio de venda a descoberto → Task 7, validado na Task 11.
- Corretora precisa de `validadaNaCvm = true` → Task 7, validado na Task 12.
- Operação imutável pro usuário comum (sem edição/exclusão exposta) → nenhum endpoint de
  PUT/DELETE em `OperacaoController` (Task 9) — por construção, não por checagem em runtime.
- ADMIN cancela só `COMPRA`, bloqueado se deixar saldo negativo, sem recalcular vendas
  passadas → Task 7 (`cancelar`), validado na Task 12.
- Endpoints e autorização (tabela do spec) → Task 9 (controllers) + Task 10 (`SecurityConfig`),
  validado nas Tasks 11-13.
- `RegraDeNegocioException` (422) → Task 4.
- Testes (todos os cenários listados no spec) → Tasks 11-13.
- Fora de escopo (endpoint de bloqueio de usuário, ações fracionárias, edição de operação,
  múltiplas carteiras, cache de posição) → nenhuma task os implementa, como pretendido.

**Placeholder scan:** nenhum "TBD"/"TODO"/"implementar depois" — todo passo tem código completo
e comandos exatos.

**Type consistency:** `PosicaoCalculator.Posicao(quantidade, precoMedio, quantidadeMinimaHistorica)`
usado de forma consistente em `OperacaoService` (`.quantidade()`, `.precoMedio()`,
`.quantidadeMinimaHistorica()`) e `CarteiraService` (`.quantidade()`, `.precoMedio()`).
`OperacaoResponseDTO`/`PosicaoDTO`/`OperacaoRequestDTO` têm os mesmos nomes de campo em toda
task que os usa (services, controllers, testes).
