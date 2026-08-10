# Cache de Posição Materializada Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar `PosicaoAtual`, uma tabela de cache materializado (carteira+ação+corretora → quantidade, preço médio) escrita na mesma transação de `OperacaoService.registrar`/`.cancelar`, pra que a leitura de posição (`GET /carteiras/me`, `GET /carteiras/{usuarioId}`) pare de recalcular do histórico completo a cada chamada — mais um endpoint de ADMIN pra reconstruir o cache de uma carteira a partir do histórico, caso ele algum dia divirja.

**Architecture:** `Operacao` continua sendo a única fonte da verdade; `PosicaoAtual` é um índice derivado, nunca escrito por fora do fluxo de operações. `PosicaoCacheService` (novo) é o único componente que escreve `PosicaoAtual` — reaproveita `PosicaoCalculator` (já existente, já testado) pra calcular, e é chamado tanto por `OperacaoService` (escrita incremental a cada operação) quanto por si mesmo (reconstrução completa). `CarteiraService` deixa de tocar em `Operacao`/`PosicaoCalculator` na leitura — vira uma query direta na tabela de cache.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA, Spring Security (session-based), H2 (dev profile, `create-drop` — sem migração necessária), JUnit 5, MockMvc.

**Spec:** `docs/superpowers/specs/2026-08-10-cache-posicao-design.md`

**Branch:** `10-cache-posicao` (já criada e com o commit do spec)

---

## Task 1: Entidade `PosicaoAtual` + `PosicaoAtualRepository`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/model/PosicaoAtual.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/repository/PosicaoAtualRepository.java`

- [ ] **Step 1: Create the `PosicaoAtual` entity**

```java
package com.curso.gestaoinvestimentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "posicoes_atuais", uniqueConstraints = @UniqueConstraint(columnNames = {"carteira_id", "acao_id", "corretora_id"}))
public class PosicaoAtual {

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

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private BigDecimal precoMedio;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

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

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoMedio() {
        return precoMedio;
    }

    public void setPrecoMedio(BigDecimal precoMedio) {
        this.precoMedio = precoMedio;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
```

- [ ] **Step 2: Create `PosicaoAtualRepository`**

```java
package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosicaoAtualRepository extends JpaRepository<PosicaoAtual, Long> {

    Optional<PosicaoAtual> findByCarteiraIdAndAcaoIdAndCorretoraId(Long carteiraId, Long acaoId, Long corretoraId);

    List<PosicaoAtual> findByCarteiraId(Long carteiraId);

    void deleteByCarteiraId(Long carteiraId);
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/model/PosicaoAtual.java src/main/java/com/curso/gestaoinvestimentos/repository/PosicaoAtualRepository.java
git commit -m "$(cat <<'EOF'
feat(carteira): entidade PosicaoAtual (cache materializado de posicao)

Uma linha por carteira+acao+corretora com quantidade e preco medio.
Constraint unica nas tres FKs. So guarda o que deriva do historico de
Operacao -- valorInvestido/valorAtual continuam calculados na leitura,
porque dependem da cotacao atual da Acao, que muda fora do fluxo de
operacoes.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: `PosicaoCacheService`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/service/PosicaoCacheService.java`

- [ ] **Step 1: Create `PosicaoCacheService`**

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantem PosicaoAtual (cache materializado) sincronizado com o historico de
 * Operacao, que continua sendo a unica fonte da verdade. atualizar() e
 * chamado no mesmo commit de OperacaoService.registrar/cancelar --
 * reconstruirCarteira() e a rede de seguranca caso cache e historico algum
 * dia divirjam.
 */
@Service
public class PosicaoCacheService {

    private final OperacaoRepository operacaoRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;

    public PosicaoCacheService(OperacaoRepository operacaoRepository, PosicaoAtualRepository posicaoAtualRepository) {
        this.operacaoRepository = operacaoRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
    }

    public void atualizar(Carteira carteira, Acao acao, Corretora corretora) {
        List<Operacao> historico = operacaoRepository.findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc(
                carteira.getId(), acao.getId(), corretora.getId(), StatusOperacao.ATIVA);
        PosicaoCalculator.Posicao calculada = PosicaoCalculator.calcular(historico);

        PosicaoAtual posicaoAtual = posicaoAtualRepository
                .findByCarteiraIdAndAcaoIdAndCorretoraId(carteira.getId(), acao.getId(), corretora.getId())
                .orElseGet(PosicaoAtual::new);
        posicaoAtual.setCarteira(carteira);
        posicaoAtual.setAcao(acao);
        posicaoAtual.setCorretora(corretora);
        posicaoAtual.setQuantidade(calculada.quantidade());
        posicaoAtual.setPrecoMedio(calculada.precoMedio());
        posicaoAtual.setAtualizadoEm(LocalDateTime.now());

        posicaoAtualRepository.save(posicaoAtual);
    }

    public void reconstruirCarteira(Carteira carteira) {
        posicaoAtualRepository.deleteByCarteiraId(carteira.getId());

        List<Operacao> ativas = operacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc(carteira.getId(), StatusOperacao.ATIVA);

        Map<String, List<Operacao>> porAcaoECorretora = new LinkedHashMap<>();
        for (Operacao operacao : ativas) {
            String chave = operacao.getAcao().getId() + "-" + operacao.getCorretora().getId();
            porAcaoECorretora.computeIfAbsent(chave, k -> new ArrayList<>()).add(operacao);
        }

        for (List<Operacao> grupo : porAcaoECorretora.values()) {
            Operacao qualquerOperacaoDoGrupo = grupo.get(0);
            atualizar(carteira, qualquerOperacaoDoGrupo.getAcao(), qualquerOperacaoDoGrupo.getCorretora());
        }
    }
}
```

Nota: `reconstruirCarteira` apaga tudo primeiro e recalcula do zero — por isso `atualizar()` sempre encontra `Optional.empty()` na primeira chamada de cada grupo durante uma reconstrução, e cria uma linha nova. Isso é intencional: reconstrução nunca é incremental, sempre confiável mesmo que o cache anterior estivesse arbitrariamente errado.

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/PosicaoCacheService.java
git commit -m "$(cat <<'EOF'
feat(carteira): PosicaoCacheService - escreve e reconstroi o cache

atualizar(): recalcula UM grupo carteira+acao+corretora via
PosicaoCalculator (reaproveitado, sem duplicar logica) e faz upsert em
PosicaoAtual. reconstruirCarteira(): apaga todo o cache daquela
carteira e recalcula do zero a partir do historico completo -- rede de
seguranca caso cache e historico algum dia divirjam. Ainda nao
conectado a OperacaoService/CarteiraService -- proximas tasks.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Conectar `OperacaoService` ao cache

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java`

- [ ] **Step 1: Injetar `PosicaoCacheService`**

Nenhum import novo é necessário — `PosicaoCacheService` já está no mesmo pacote
`com.curso.gestaoinvestimentos.service` que `OperacaoService`.

Troque o campo e o construtor:

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

- [ ] **Step 2: Atualizar o cache em `registrar()`, logo após salvar a operação**

Encontre este trecho (final de `registrar()`):

```java
        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    public List<OperacaoResponseDTO> listarProprias(String emailUsuarioAutenticado) {
```

Substitua por:

```java
        Operacao salva = operacaoRepository.save(operacao);
        posicaoCacheService.atualizar(carteira, acao, corretora);
        return toResponseDTO(salva);
    }

    public List<OperacaoResponseDTO> listarProprias(String emailUsuarioAutenticado) {
```

- [ ] **Step 3: Atualizar o cache em `cancelar()`, logo após salvar a operação**

Encontre este trecho (final de `cancelar()`):

```java
        Operacao salva = operacaoRepository.save(operacao);
        return toResponseDTO(salva);
    }

    private List<OperacaoResponseDTO> listarPorCarteira(Carteira carteira) {
```

Substitua por:

```java
        Operacao salva = operacaoRepository.save(operacao);
        posicaoCacheService.atualizar(operacao.getCarteira(), operacao.getAcao(), operacao.getCorretora());
        return toResponseDTO(salva);
    }

    private List<OperacaoResponseDTO> listarPorCarteira(Carteira carteira) {
```

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 5: Run the existing Operacao test suite to confirm nada quebrou ainda**

(Ainda vai passar exatamente como antes — o cache está sendo escrito, mas `CarteiraService` ainda não lê dele. Este passo só confirma que a injeção de `PosicaoCacheService` não quebrou o fluxo de `registrar`/`cancelar`.)

Run: `./mvnw.cmd -q -Dtest=OperacaoIntegrationTest test`
Expected: no output, exit code 0. Confirm with:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.OperacaoIntegrationTest.txt`
Expected: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/OperacaoService.java
git commit -m "$(cat <<'EOF'
feat(carteira): OperacaoService escreve no cache a cada registrar/cancelar

posicaoCacheService.atualizar() chamado logo apos salvar a Operacao,
na mesma transacao ja existente (@Transactional) de registrar() e
cancelar(). CarteiraService ainda le do historico completo -- proxima
task troca a leitura pra vir do cache.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: `CarteiraService` passa a ler do cache

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/CarteiraService.java`

- [ ] **Step 1: Substituir o arquivo inteiro**

Troque todo o conteúdo de `CarteiraService.java` por:

```java
package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.dto.PosicaoDTO;
import com.curso.gestaoinvestimentos.exception.RecursoNaoEncontradoException;
import com.curso.gestaoinvestimentos.model.Acao;
import com.curso.gestaoinvestimentos.model.Carteira;
import com.curso.gestaoinvestimentos.model.Corretora;
import com.curso.gestaoinvestimentos.model.PosicaoAtual;
import com.curso.gestaoinvestimentos.model.Usuario;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final PosicaoAtualRepository posicaoAtualRepository;
    private final UsuarioRepository usuarioRepository;
    private final PosicaoCacheService posicaoCacheService;

    public CarteiraService(CarteiraRepository carteiraRepository, PosicaoAtualRepository posicaoAtualRepository,
                            UsuarioRepository usuarioRepository, PosicaoCacheService posicaoCacheService) {
        this.carteiraRepository = carteiraRepository;
        this.posicaoAtualRepository = posicaoAtualRepository;
        this.usuarioRepository = usuarioRepository;
        this.posicaoCacheService = posicaoCacheService;
    }

    public List<PosicaoDTO> buscarPosicaoPropria(String emailUsuarioAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + emailUsuarioAutenticado));
        return buscarPosicaoPorUsuarioId(usuario.getId());
    }

    public List<PosicaoDTO> buscarPosicaoPorUsuarioId(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));

        List<PosicaoAtual> posicoesEmCache = posicaoAtualRepository.findByCarteiraId(carteira.getId());

        List<PosicaoDTO> posicoes = new ArrayList<>();
        for (PosicaoAtual posicaoAtual : posicoesEmCache) {
            if (posicaoAtual.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Acao acao = posicaoAtual.getAcao();
            Corretora corretora = posicaoAtual.getCorretora();
            BigDecimal valorInvestido = posicaoAtual.getPrecoMedio().multiply(posicaoAtual.getQuantidade());
            BigDecimal valorAtual = acao.getCotacaoAtual() == null
                    ? null
                    : acao.getCotacaoAtual().multiply(posicaoAtual.getQuantidade());

            posicoes.add(new PosicaoDTO(
                    acao.getTicker(),
                    corretora.getNomeFantasia(),
                    posicaoAtual.getQuantidade(),
                    posicaoAtual.getPrecoMedio(),
                    valorInvestido,
                    valorAtual
            ));
        }
        return posicoes;
    }

    public List<PosicaoDTO> reconstruirPosicao(Long usuarioId) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carteira nao encontrada para o usuario " + usuarioId));
        posicaoCacheService.reconstruirCarteira(carteira);
        return buscarPosicaoPorUsuarioId(usuarioId);
    }
}
```

Note: `OperacaoRepository` e `PosicaoCalculator` saíram completamente de `CarteiraService` — a leitura de posição não toca mais em `Operacao`.

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 3: Run the full existing test suite — este é o checkpoint de regressão**

Os 9 cenários de `OperacaoIntegrationTest` fazem `POST /operacoes` seguido de `GET /carteiras/me` — continuarem verdes prova que o cache está sendo escrito certo em compra simples, venda com preço médio, venda a descoberto bloqueada, corretora não validada, cancelamento, cancelamento bloqueado, zerar-e-recomeçar e fracionário, todos sem tocar em `PosicaoCalculator` na leitura.

Run: `./mvnw.cmd -q test`
Expected: no output, exit code 0. Confirm com:
Run: `grep -E "Tests run|Test set" target/surefire-reports/*.txt` (bash) ou equivalente
Expected:
```
Test set: com.curso.gestaoinvestimentos.GestaoInvestimentosApplicationTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.OperacaoIntegrationTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.UsuarioAuthIntegrationTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.service.CorretoraServiceTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.service.PosicaoCalculatorTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, ...
```

Se algum cenário de `OperacaoIntegrationTest` falhar aqui, o bug está em `PosicaoCacheService.atualizar` ou na troca de leitura em `CarteiraService` — não avance pra próxima task sem isso verde, já que as próximas tasks assumem que o cache está correto.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/CarteiraService.java
git commit -m "$(cat <<'EOF'
feat(carteira): CarteiraService le a posicao do cache, nao mais do historico

buscarPosicaoPorUsuarioId vira uma query direta em PosicaoAtual --
OperacaoRepository e PosicaoCalculator saem completamente de
CarteiraService. valorInvestido/valorAtual continuam calculados na
leitura (dependem da cotacao atual, que muda fora do fluxo de
operacoes). Ganha reconstruirPosicao(usuarioId), delegando pra
PosicaoCacheService -- ainda sem endpoint, proxima task.

Os 9 cenarios de OperacaoIntegrationTest continuam verdes lendo do
cache em vez do historico completo, provando que o cache reflete
corretamente compra, venda, cancelamento e fracionario.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Endpoint de reconstrução (`CarteiraController` + `SecurityConfig`)

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/controller/CarteiraController.java`
- Modify: `src/main/java/com/curso/gestaoinvestimentos/security/SecurityConfig.java`

- [ ] **Step 1: Adicionar o endpoint em `CarteiraController`**

Encontre o final da classe:

```java
    @GetMapping("/{usuarioId}/operacoes")
    public List<OperacaoResponseDTO> operacoesPorUsuario(@PathVariable Long usuarioId) {
        return operacaoService.listarComoAdmin(usuarioId);
    }
}
```

Substitua por:

```java
    @GetMapping("/{usuarioId}/operacoes")
    public List<OperacaoResponseDTO> operacoesPorUsuario(@PathVariable Long usuarioId) {
        return operacaoService.listarComoAdmin(usuarioId);
    }

    @PatchMapping("/{usuarioId}/reconstruir")
    public List<PosicaoDTO> reconstruir(@PathVariable Long usuarioId) {
        return carteiraService.reconstruirPosicao(usuarioId);
    }
}
```

(`PatchMapping` já vem do `import org.springframework.web.bind.annotation.*;` existente — nenhum import novo necessário.)

- [ ] **Step 2: Restringir a rota a ADMIN em `SecurityConfig`**

Encontre:

```java
                        .requestMatchers(HttpMethod.GET, "/carteiras/*", "/carteiras/*/operacoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/operacoes/*/cancelar").hasRole("ADMIN")
```

Substitua por:

```java
                        .requestMatchers(HttpMethod.GET, "/carteiras/*", "/carteiras/*/operacoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/carteiras/*/reconstruir").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/operacoes/*/cancelar").hasRole("ADMIN")
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw.cmd -q compile`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/controller/CarteiraController.java src/main/java/com/curso/gestaoinvestimentos/security/SecurityConfig.java
git commit -m "$(cat <<'EOF'
feat(carteira): endpoint de ADMIN para reconstruir cache de posicao

PATCH /carteiras/{usuarioId}/reconstruir, restrito a ADMIN mesmo
padrao de PATCH /operacoes/*/cancelar. Retorna a posicao ja
recalculada (List<PosicaoDTO>), mesmo padrao de OperacaoController
.cancelar que retorna o recurso atualizado em vez de 204.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Testes — reconstrução após divergência e autorização

**Files:**
- Modify: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

- [ ] **Step 1: Adicionar o import e o repository do cache**

Encontre os imports de repository:

```java
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
```

Substitua por:

```java
import com.curso.gestaoinvestimentos.repository.AcaoRepository;
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.CorretoraRepository;
import com.curso.gestaoinvestimentos.repository.OperacaoRepository;
import com.curso.gestaoinvestimentos.repository.PosicaoAtualRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
```

- [ ] **Step 2: Injetar o repository e apagar `PosicaoAtual` na limpeza do banco**

Encontre:

```java
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
```

Substitua por:

```java
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
```

`PosicaoAtual` tem as mesmas FKs (`carteira`, `acao`, `corretora`) que `Operacao` — por isso precisa ser apagada antes delas, mesmo motivo já documentado pra `Operacao` no comentário de `cadastrarAcao`.

- [ ] **Step 3: Adicionar os dois testes novos**

Encontre o final do arquivo:

```java
        assertEquals(1, posicoes.length);
        assertEquals(0, posicoes[0].quantidade().compareTo(new BigDecimal("0.5")));
    }
}
```

Substitua por:

```java
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
}
```

- [ ] **Step 4: Run this test class**

Run: `./mvnw.cmd -q -Dtest=OperacaoIntegrationTest test`
Expected: no output, exit code 0. Confirm com:
Run: `cat target/surefire-reports/com.curso.gestaoinvestimentos.OperacaoIntegrationTest.txt`
Expected: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Run the full project test suite**

Run: `./mvnw.cmd -q test`
Expected: no output, exit code 0. Confirm com:
Run: `grep -E "Tests run|Test set" target/surefire-reports/*.txt`
Expected:
```
Test set: com.curso.gestaoinvestimentos.GestaoInvestimentosApplicationTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.OperacaoIntegrationTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.UsuarioAuthIntegrationTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.service.CorretoraServiceTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, ...
Test set: com.curso.gestaoinvestimentos.service.PosicaoCalculatorTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, ...
```

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "$(cat <<'EOF'
test(carteira): reconstrucao de cache apos divergencia e autorizacao

Cobre: apagar PosicaoAtual direto pelo repository (simulando
divergencia) faz GET /carteiras/me vir vazio, confirmando que a
leitura realmente vem do cache e nao recalcula do historico; PATCH
/carteiras/{id}/reconstruir restaura o valor correto; usuario comum
tentando reconstruir leva 403.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage:**
- `PosicaoAtual` (carteira+ação+corretora → quantidade, precoMedio, atualizadoEm), constraint única → Task 1.
- Escrita no mesmo commit de `registrar`/`cancelar` → Task 3.
- Leitura vindo do cache, `valorInvestido`/`valorAtual` calculados na leitura → Task 4.
- Endpoint de reconstrução, escopo por carteira, restrito a ADMIN → Task 5.
- Reconstrução em lote e cache de `valorInvestido`/`valorAtual` — fora de escopo, nenhuma task os implementa, como pretendido.
- Testes de regressão (suíte existente) + reconstrução + autorização → Task 4 Step 3 (regressão) e Task 6 (novos).

**Placeholder scan:** nenhum "TBD"/"TODO"/"implementar depois" — todo passo tem código completo e comandos exatos.

**Type consistency:** `PosicaoAtual.quantidade`/`.precoMedio` são `BigDecimal` em toda a cadeia (entidade, `PosicaoCacheService`, `CarteiraService`, `PosicaoDTO`) — mesmos tipos que `PosicaoCalculator.Posicao` já usa desde a feature de ações fracionárias. Nenhuma conversão de nome entre camadas (`quantidade`, `precoMedio` idênticos em todo lugar).
