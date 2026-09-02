# Rendo — Plataforma de Gestão de Investimentos

Rendo é uma aplicação web acadêmica para registro e acompanhamento de uma carteira pessoal de investimentos: cadastro de ações e corretoras, lançamento de operações de compra/venda, cálculo de saldo e posição atual, cotações em tempo real via APIs externas e histórico de rendimento. O projeto foi desenvolvido como trabalho de curso e evoluiu de forma incremental, com cada funcionalidade documentada como uma "mudança" (proposta → design → tarefas → especificação) antes de ser implementada — ver [Metodologia de desenvolvimento (OpenSpec)](#metodologia-de-desenvolvimento-openspec).

## Sumário

- [Para quem está avaliando o projeto](#para-quem-está-avaliando-o-projeto)
- [O que a aplicação faz](#o-que-a-aplicação-faz)
- [Arquitetura](#arquitetura)
- [Stack técnica](#stack-técnica)
- [Modelo de domínio](#modelo-de-domínio)
- [API REST](#api-rest)
- [Integrações externas e resiliência](#integrações-externas-e-resiliência)
- [Segurança e autenticação](#segurança-e-autenticação)
- [Como rodar o projeto](#como-rodar-o-projeto)
- [Testes](#testes)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Metodologia de desenvolvimento (OpenSpec)](#metodologia-de-desenvolvimento-openspec)
- [Grafo de conhecimento do código (Graphify)](#grafo-de-conhecimento-do-código-graphify)
- [Decisões de design notáveis](#decisões-de-design-notáveis)

## Para quem está avaliando o projeto

Se você está lendo este README para avaliar o projeto (ex.: um professor ou revisor), os pontos mais relevantes são:

1. **É uma aplicação full-stack real**, não um CRUD de exemplo: há autenticação (formulário próprio + login social OAuth2), integração com três APIs externas de terceiros, tolerância a falha dessas integrações (circuit breaker + retry + cache), verificação de email assíncrona e cálculo de domínio não trivial (posição consolidada, saldo, rendimento histórico, conversão de moeda).
2. **O domínio é auditável por natureza**: toda operação de compra/venda é um registro imutável (nunca é editada ou apagada, só cancelada), e a posição/saldo atuais são *derivados* recalculando o histórico — o mesmo princípio contábil de um livro-razão. Isso está detalhado em [Modelo de domínio](#modelo-de-domínio).
3. **Todo requisito de negócio tem um documento de rastreabilidade** em `openspec/specs/`, escrito antes do código correspondente existir. Isso permite conferir, para qualquer regra do sistema, *por que* ela existe e não apenas *que* ela existe — ver [Metodologia de desenvolvimento](#metodologia-de-desenvolvimento-openspec).
4. **O código tem testes automatizados** (unitários e de integração) cobrindo as regras de negócio mais sensíveis a erro: cálculo de saldo, cálculo de posição, validação de CPF, verificação de email. Ver [Testes](#testes).

## O que a aplicação faz

Do ponto de vista de um usuário:

- Cria conta (cadastro tradicional com confirmação de email por código, ou login social via Google/Microsoft).
- Cadastra as corretoras onde investe (validadas por CNPJ real via BrasilAPI) e as ações/ativos que acompanha (com validação de ticker e de compatibilidade entre mercado e moeda).
- Registra operações de compra e venda; o sistema calcula automaticamente o saldo disponível e a posição atual de cada ativo.
- Acompanha cotações atualizadas (mercado brasileiro via brapi, mercado americano via Twelve Data, câmbio USD→BRL via DolarAPI) e um gráfico de rendimento histórico da carteira.
- Vê tudo consolidado num dashboard (visão geral, posições, catálogo de ações, catálogo de corretoras), com suporte a tema claro/escuro.

## Arquitetura

Aplicação **monolítica em camadas**, servidor Java (Spring Boot) renderizando HTML no servidor (Thymeleaf) para as páginas e expondo uma **API REST em JSON** consumida pelo próprio frontend via JavaScript puro (sem framework SPA).

```
Navegador (Thymeleaf + JS vanilla)
        │  HTTP (form-login / fetch JSON)
        ▼
┌─────────────────────────────────────────────┐
│                Controller                    │  @RestController — valida entrada,
│  Usuario / Carteira / Acao / Corretora /     │  mapeia DTO ↔ domínio, define rotas HTTP
│  Operacao / Mercado / Pagina                 │
└───────────────────┬───────────────────────────┘
                     ▼
┌─────────────────────────────────────────────┐
│                  Service                     │  regra de negócio: cálculo de saldo/posição,
│  UsuarioService, CarteiraService,             │  validações de domínio, orquestração de
│  OperacaoService, AcaoService,                │  integrações externas
│  RendimentoService, EmailService...          │
└───────┬───────────────────────┬───────────────┘
        ▼                       ▼
┌───────────────┐     ┌───────────────────────┐
│  Repository    │     │      Integration       │  clients HTTP para APIs de terceiros,
│  (Spring Data  │     │  BrapiCotacaoProvider,  │  cada chamada protegida por
│   JPA)         │     │  TwelveDataCotacao...,  │  @CircuitBreaker (Resilience4j)
└───────┬────────┘     │  DolarApiCambioClient,  │
        ▼              │  BrasilApiCnpjClient,   │
┌───────────────┐      │  ViaCepClient           │
│  H2 (dev) /    │      └───────────────────────┘
│  PostgreSQL    │
│  (docker)      │
└───────────────┘
```

**Por que essa arquitetura, e não uma SPA com API separada?** É um projeto acadêmico de porte pequeno/médio, com um único frontend consumidor. Uma separação front/back em processos distintos (React + API) adicionaria complexidade de build, CORS e deploy sem benefício real neste estágio — decisão documentada como YAGNI em várias das mudanças arquiteturais (ver [openspec/changes/archive](openspec/changes/archive)).

## Stack técnica

| Camada | Tecnologia | Observação |
|---|---|---|
| Linguagem/runtime | Java 17 | |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security, OAuth2 Client, Cache, Mail) | |
| Views server-side | Thymeleaf | páginas `login`, `dashboard`, `index` |
| Frontend | HTML/CSS/JS vanilla + Bootstrap 5 (webjar) + SweetAlert2 (vendorizado, sem CDN) | sem bundler/build step |
| Persistência | Spring Data JPA + Hibernate | H2 em memória (dev/teste), PostgreSQL (docker/produção) |
| Resiliência | Resilience4j (circuit breaker) | uma instância por integração externa |
| Segurança | Spring Security (form login + OAuth2 login) | Google e Microsoft como provedores OIDC |
| Email | Spring Mail (JavaMailSender) + Thymeleaf (template do email) | SMTP configurável, pode ser desativado em dev |
| Empacotamento | Maven (`mvnw`) | |
| Containerização | Docker multi-stage (Maven build → JRE runtime) + Docker Compose (app + PostgreSQL) | |
| Testes | JUnit 5, Mockito, Spring Boot Test, Spring Security Test | |
| APIs externas consumidas | [brapi](https://brapi.dev/) (cotações B3), [Twelve Data](https://twelvedata.com/) (cotações EUA), [DolarAPI](https://dolarapi.com/) (câmbio USD→BRL), [BrasilAPI](https://brasilapi.com.br/) (CNPJ), [ViaCEP](https://viacep.com.br/) (CEP) | todas com fallback gracioso quando indisponíveis |

## Modelo de domínio

O núcleo do domínio segue um padrão de **livro-razão imutável** (append-only ledger), o mesmo princípio usado em contabilidade e em sistemas bancários: em vez de guardar "o saldo atual" e "a posição atual" como campos que são editados a cada operação, o sistema guarda o **histórico completo de operações** e deriva saldo/posição recalculando esse histórico. Isso elimina uma classe inteira de bugs de inconsistência (saldo dessincronizado da soma das operações) ao custo de mais uma leitura no banco — uma troca deliberada e documentada em `docs/superpowers/plans/2026-08-09-carteira-investimentos/`.

Entidades principais:

- **`Usuario`** — conta do investidor. Guarda credenciais (para login tradicional), estado de verificação de email e vínculo opcional com provedor OAuth2 (Google/Microsoft).
- **`Corretora`** — instituição financeira onde o usuário opera, validada por CNPJ real (BrasilAPI) no cadastro.
- **`Acao`** — um ativo negociável (ação, ETF etc.), identificado por ticker, com mercado (`Mercado`: Brasil ou EUA) e moeda de cotação correspondente.
- **`Operacao`** — **o registro imutável central**: uma compra ou venda de uma quantidade de uma `Acao`, numa `Corretora`, por um `Usuario`, num preço e data. Uma vez criada, nunca é editada — só pode ser **cancelada** (`StatusOperacao.CANCELADA`), o que a exclui dos cálculos sem apagar o rastro. Para ações negociadas em dólar, grava também `taxaCambio` — a cotação USD→BRL vigente no momento da operação, capturada de forma imutável para que o histórico não mude retroativamente se o câmbio variar depois.
- **`Carteira`** — agrega as operações de um usuário; `saldo` e `posição por ativo` são **calculados sob demanda** a partir de `OperacaoRepository`, nunca armazenados como estado mutável (`SaldoCalculator`, `PosicaoCalculator`).
- **`PosicaoAtual`** — view materializada/cache da posição corrente por ativo, usada para leitura rápida no dashboard (`PosicaoCacheService`), sempre reconstruível a partir de `Operacao` (endpoint `PATCH /carteiras/{usuarioId}/reconstruir`).
- **`HistoricoCotacao`** — snapshot histórico de cotação de uma `Acao`, capturado a cada consulta/atualização de preço, usado para reconstruir o gráfico de rendimento ao longo do tempo (`RendimentoService`).
- **`EmailVerificationCode`** — código de verificação de email de curta duração, associado a um `Usuario` e a um `TipoCodigo` (cadastro ou reenvio).

## API REST

Todas as rotas abaixo respondem em JSON (exceto `PaginaController`, que serve as páginas HTML). Autenticação é feita via sessão (form login ou OAuth2); rotas sob `/carteiras/me/**` operam sobre o usuário autenticado.

| Recurso | Rotas | Descrição |
|---|---|---|
| Usuários | `POST /usuarios` · `POST /usuarios/verificar-email` · `POST /usuarios/reenviar-codigo` · `GET /usuarios` · `GET /usuarios/{id}` · `PATCH /usuarios/{id}/bloquear` · `PATCH /usuarios/{id}/desbloquear` | cadastro, verificação de email por código, listagem administrativa |
| Corretoras | `POST /corretoras` · `GET /corretoras` · `GET /corretoras/{id}` · `GET /corretoras/cnpj/{cnpj}` | cadastro validado por CNPJ real |
| Ações | `POST /acoes` · `GET /acoes` · `GET /acoes/{id}` · `GET /acoes/ticker/{ticker}` · `PUT /acoes/{id}/atualizar-cotacao` · `GET /acoes/{id}/historico` · `DELETE /acoes/{id}` | catálogo de ativos; exclusão bloqueada se houver operações/posições associadas |
| Operações | `POST /operacoes` · `PATCH /operacoes/{id}/cancelar` | lançamento de compra/venda; cancelamento em vez de edição/exclusão |
| Carteira | `GET /carteiras/me` · `GET /carteiras/me/operacoes` · `GET /carteiras/me/saldo` · `GET /carteiras/me/rendimento-historico` · `GET /carteiras/{usuarioId}` · `GET /carteiras/{usuarioId}/operacoes` · `PATCH /carteiras/{usuarioId}/reconstruir` | posição/saldo/rendimento do usuário autenticado (`/me`) e visão administrativa por id |
| Mercado | `GET /mercado/ibovespa` | índice Ibovespa em tempo real, para o widget do dashboard |
| Páginas | `GET /login` · `GET /dashboard` | páginas Thymeleaf |

Erros de negócio (recurso não encontrado, regra de negócio violada, integração externa indisponível) são tratados centralizadamente em `GlobalExceptionHandler` (`@RestControllerAdvice`), retornando um `ErrorResponse` consistente com status HTTP apropriado (404, 422, 503 etc.) em vez de stack traces.

## Integrações externas e resiliência

O sistema depende de quatro APIs de terceiros que estão fora do seu controle e podem falhar ou ficar lentas. Cada integração é isolada atrás de uma interface (`CotacaoProvider`, `CnpjClient`, `CepClient`) e protegida por um **circuit breaker** dedicado (Resilience4j), configurado em `application.properties`:

- Janela deslizante de 10 chamadas, limiar de 50% de falha para abrir o circuito, 30s em estado aberto antes de testar novamente.
- Um "recurso não encontrado" (CNPJ inválido, ticker inexistente) é tratado como **resultado de negócio, não falha de infraestrutura** — `RecursoNaoEncontradoException` é explicitamente excluída da contagem de falhas que abre o circuito, para que um usuário digitando um CNPJ errado não derrube a integração para todo mundo.
- Retry manual (`RetryExterno`) apenas em falhas transientes (timeout, 5xx), nunca em erros 4xx.
- **Degradação graciosa no caminho de leitura**: se a cotação de uma ação não puder ser obtida, o dashboard mostra o último valor conhecido (histórico) em vez de quebrar a página. No caminho de **escrita** (registrar uma operação), a falha é reportada ao usuário, pois usar um preço desatualizado para uma compra/venda real seria pior que recusar a operação. Essa assimetria é uma decisão de design deliberada, documentada em `docs/superpowers/specs/2026-08-19-conversao-cambio-eua-design.md`.
- Cache (Spring Cache) para consultas de CNPJ/CEP, que raramente mudam.

## Segurança e autenticação

- **Spring Security** com dois fluxos de login: formulário tradicional (usuário/senha, com senha hasheada) e **OAuth2/OIDC social** (Google e Microsoft) — Apple foi avaliado e removido (`remove-login-social-apple`) por exigir infraestrutura de app assinado incompatível com o escopo acadêmico do projeto.
- Login social é **opcional por configuração**: sem as variáveis `GOOGLE_CLIENT_ID`/`MICROSOFT_CLIENT_ID` no `.env`, os botões correspondentes simplesmente não aparecem (bean condicional via `@ConditionalOnExpression`) — a aplicação sobe normalmente sem eles.
- **Verificação de email obrigatória** após cadastro tradicional: um código de 6 dígitos é gerado, enviado por email (ou logado, se `MAIL_ENABLED=false`) e deve ser confirmado antes do primeiro login (`EmailNaoVerificadoException` bloqueia o `UserDetailsService` até a confirmação).
- CPF é validado (dígito verificador) e único por usuário — `@CPF` é uma anotação de Bean Validation customizada (`CpfValidator`).

## Como rodar o projeto

### Opção 1 — Docker Compose (recomendado, sobe app + PostgreSQL)

```bash
cp .env.example .env        # ajuste POSTGRES_PASSWORD e as chaves opcionais
docker compose up --build
```

A aplicação sobe em `http://localhost:8080` (porta configurável via `APP_PORT`).

### Opção 2 — Local com Maven (usa H2 em memória, sem Docker)

```bash
./mvnw spring-boot:run       # Linux/macOS
mvnw.cmd spring-boot:run     # Windows
```

Por padrão roda no profile `dev`: banco H2 em memória (`spring.jpa.hibernate.ddl-auto=create-drop`, dados zerados a cada restart), console H2 disponível em `/h2-console`, envio de email desativado (`MAIL_ENABLED=false` — o código de verificação é gerado e aparece no log, sem precisar de SMTP).

Variáveis de ambiente opcionais (todas com default seguro se omitidas — ver `.env.example`): chaves das APIs externas (`BRAPI_API_TOKEN`, `TWELVEDATA_API_KEY`), credenciais de login social e configuração de SMTP.

## Testes

```bash
./mvnw test
```

Suíte com testes **unitários** (`SaldoCalculatorTest`, `PosicaoCalculatorTest`, `CpfValidatorTest`, `AcaoServiceTest`, `CorretoraServiceTest`, `EmailVerificationServiceTest`) para as regras de cálculo e validação mais sensíveis a erro, e **testes de integração** com contexto Spring completo (`OperacaoIntegrationTest`, `UsuarioAuthIntegrationTest`) exercitando o fluxo ponta a ponta via HTTP contra o banco H2 em memória. Há ainda um teste de marcação HTML (`DashboardAcoesMarkupTest`) que verifica a estrutura da tabela de posições renderizada pelo Thymeleaf.

## Estrutura de pastas

```
src/main/java/com/curso/gestaoinvestimentos/
├── controller/     endpoints REST + páginas Thymeleaf
├── service/        regra de negócio (cálculo, orquestração)
├── repository/     Spring Data JPA
├── model/          entidades JPA
├── dto/            objetos de request/response da API
├── integration/    clients para as APIs externas (brapi, Twelve Data, DolarAPI, BrasilAPI, ViaCEP)
├── security/       Spring Security (form login, OAuth2, verificação de email)
├── validation/      Bean Validation customizada (@CPF)
└── exception/       exceções de domínio + handler global

src/main/resources/
├── templates/       páginas Thymeleaf (login, dashboard) + fragments reutilizáveis
├── static/          CSS e JS vanilla (sem build step; SweetAlert2 vendorizado)
└── application*.properties   configuração base + profiles dev/docker

openspec/            especificação viva de cada capacidade do sistema (ver seção abaixo)
docs/superpowers/    propostas, designs e planos de implementação históricos
graphify-out/        grafo de conhecimento do código, gerado automaticamente (ver seção abaixo)
```

## Metodologia de desenvolvimento (OpenSpec)

Este projeto usa **spec-driven development**: cada funcionalidade nasce como uma "mudança" (`change`) em `openspec/changes/`, com quatro artefatos escritos **antes** do código — `proposal.md` (o quê e por quê), `design.md` (decisões técnicas e trade-offs, quando não triviais), `tasks.md` (checklist de implementação) e um `spec` delta (o comportamento observável, em cenários Given/When/Then). Depois de implementada e verificada, a mudança é **arquivada** e seu spec delta é fundido no spec permanente da capacidade em `openspec/specs/`.

Isso significa que `openspec/specs/` é hoje a **fonte de verdade viva dos requisitos do sistema** — por exemplo, `openspec/specs/conversao-cambio-eua/` documenta exatamente por que ações americanas são convertidas para reais na tela mas o saldo é debitado pelo valor original, e `openspec/specs/cache-resiliencia-integracoes/` documenta as regras de circuit breaker descritas acima. Todas as 18 capacidades do sistema estão hoje arquivadas e refletidas nos specs; o histórico completo de decisão de cada uma (incluindo alternativas descartadas e por quê) fica em `openspec/changes/archive/` e `docs/superpowers/`.

Para quem quer entender **por que** uma regra existe (não só o que ela faz), o caminho é: `openspec/specs/<capacidade>/spec.md` (comportamento atual) → `openspec/changes/archive/<data>-<capacidade>/design.md` (raciocínio por trás da decisão).

## Grafo de conhecimento do código (Graphify)

O repositório mantém em `graphify-out/` um **grafo de conhecimento** gerado automaticamente a partir do código-fonte e dos documentos do OpenSpec (ferramenta [graphify](https://github.com/safishamsi/graphify)). Ele liga entidades de código (classes, métodos) a requisitos e decisões de design que as motivaram, permitindo consultas como "por que essa exceção existe" ou "que decisão levou a essa estrutura de dados" com a origem exata da informação (arquivo e linha) — útil tanto para navegação por um novo desenvolvedor quanto para auditoria de rastreabilidade requisito→código.

- `graphify-out/graph.html` — visualização interativa do grafo (abrir no navegador).
- `graphify-out/GRAPH_REPORT.md` — relatório em texto: comunidades de código detectadas, nós mais conectados ("god nodes"), conexões inesperadas entre documentação e implementação.
- `graphify-out/graph.json` — dados brutos do grafo, formato consumível por ferramentas de RAG.

O grafo é atualizado incrementalmente a cada lote de mudanças (`graphify update .`), sem custo de LLM para código (extração estrutural via AST); a extração semântica dos documentos do OpenSpec usa o próprio agente de IA como LLM quando nenhuma chave do Gemini está configurada.

## Decisões de design notáveis

Uma amostra de decisões arquiteturais deliberadas, com a lógica YAGNI (não construir o que não é necessário ainda) aplicada explicitamente e documentada:

- **Sem strategy pattern para clients de cotação/câmbio de fonte única** — cada integração (brapi, Twelve Data, DolarAPI) tem só um provedor real hoje; uma interface de estratégia intercambiável seria abstração especulativa até existir um segundo provedor.
- **Sem cache/agendamento para a taxa de câmbio** — a cotação USD→BRL é buscada sob demanda a cada operação, não periodicamente; o volume de escrita do sistema não justifica a complexidade de um job agendado.
- **Sem tabela de série temporal dedicada para rendimento** — o gráfico de rendimento é computado por leitura (`RendimentoService`), reconstruindo pontos a partir do histórico de operações e cotações, em vez de manter uma tabela pré-agregada que precisaria ser mantida em sincronia.
- **Gráfico de rendimento desenhado em SVG puro**, sem biblioteca de charting — o caso de uso (uma linha, poucos pontos) não justificava uma dependência nova.
- **SweetAlert2 vendorizado localmente** (sem CDN, sem etapa de build) — consistente com a escolha de manter o frontend sem bundler.

Essas e outras decisões — incluindo as alternativas descartadas e o raciocínio completo — estão documentadas em `openspec/changes/archive/*/design.md`.

---

*Rendo — projeto acadêmico de gestão e simulação de investimentos.*
