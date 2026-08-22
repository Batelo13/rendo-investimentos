## Context

Hoje `AcaoService.listar()`, `CorretoraService.listar()`, `UsuarioService.listar()` e `OperacaoService.listarPorCarteira()` chamam `repository.findAll()`/`findByCarteiraIdOrderByDataHoraDesc(id)` sem paginação, e os controllers retornam `List<DTO>`. Todo `JpaRepository` já estende `PagingAndSortingRepository`, então `findAll(Pageable)` já existe sem código novo. Ver proposal.md - Why.

`dashboard.js` usa `state.acoes`/`state.corretoras` como cache completo em vários pontos (dropdown de corretora no modal de compra, lookup de moeda por ticker em Posições/Operações, botões de detalhe) — não só para desenhar a tabela do catálogo. Paginar essas duas de verdade no frontend exigiria refatorar todos esses pontos para buscar dados sob demanda em vez de a partir do cache local.

## Goals / Non-Goals

**Goals:**
- Backend paginado de verdade (query com `LIMIT`/`OFFSET`) nos 5 endpoints listados na proposta.
- Navegação de página real na tabela de Histórico de operações no dashboard — o caso com maior necessidade real, por ser histórico imutável e sem limite de crescimento.
- Preservar 100% do comportamento atual de cálculo (posição, saldo, rendimento), que depende do histórico completo.

**Non-Goals:**
- Paginação de verdade na UI de Ações/Corretoras — ficaria para uma change futura que também resolva o cache completo usado pelo dropdown/lookup (ver Riscos).
- Busca/filtro server-side — o filtro de texto já existente em cada tabela continua client-side, agora operando sobre o conteúdo já carregado (página atual, no caso de Operações).
- Ordenação customizável pelo usuário via UI — o parâmetro `sort` fica disponível na API (recurso nativo do Spring Data) mas não fica exposto em nenhum controle visual nesta change.

## Decisions

- **`Pageable` resolvido automaticamente pelo Spring MVC**: os controllers recebem `Pageable pageable` como parâmetro (com `@PageableDefault(size = 20)`), sem parsear `page`/`size` manualmente — é um recurso nativo do Spring Data Web já disponível via `spring-boot-starter-web` + `spring-boot-starter-data-jpa`, nenhuma dependência nova.
- **`Page<Entity>.map(...)` para DTOs**: cada `Service.listar()` troca `.stream().map(...).toList()` por `.map(...)` sobre o `Page<Entity>` retornado pelo repository — `Page` já tem `.map()`, preservando os metadados de paginação sem reconstruir o objeto na mão.
- **Novo overload no `OperacaoRepository`** para a query já existente (`findByCarteiraIdOrderByDataHoraDesc`), aceitando `Pageable` e retornando `Page<Operacao>`. A query derivada usada pelos cálculos (`findByCarteiraIdAndStatusOrderByDataHoraAsc`) não é tocada — já está isolada em métodos próprios, então não corre risco de ganhar paginação por engano.
- **Ações/Corretoras: só o backend ganha paginação, frontend pede uma página grande (`size` alto)**: decisão explícita do usuário para não precisar refatorar o dropdown do modal de compra e os lookups de moeda/detalhe agora. A API fica pronta para paginação de verdade quando o frontend for revisitado.
- **`AcaoService.listar()` calcula a taxa de câmbio olhando só a página atual**: hoje o método varre a lista inteira em busca de alguma ação EUA antes de decidir se busca a taxa de câmbio. Com paginação, essa verificação passa a olhar só os itens da página — como o frontend de Ações pede uma página grande (ver acima), na prática continua vendo o catálogo inteiro; se o `size` do pedido for pequeno, o comportamento passa a ser por página, não global — mudança aceitável porque é exatamente o que "paginar" significa.

## Risks / Trade-offs

- [Ações/Corretoras: `dashboard.js` pede uma página grande (`size=1000`, por exemplo) para preservar o cache completo, então o ganho de performance de rede não se aplica a esses dois catálogos ainda] → mitigação: documentado como Non-Goal; o backend já suporta paginação de verdade quando uma change futura resolver o cache completo do frontend.
- [Resposta de array vira objeto paginado — mudança de contrato, **BREAKING**, para qualquer client desses 5 endpoints] → mitigação: só existe um client (o próprio `dashboard.js`), atualizado nesta mesma change; nenhuma integração externa depende desses endpoints hoje.
- [`AcaoServiceTest` mocka `repository.findAll()` sem argumento] → precisa mudar para `findAll(any(Pageable.class))` retornando `PageImpl<>(...)`; sem isso os testes existentes quebram.

## Migration Plan

Mudança é aditiva a nível de banco (nenhuma migração de schema) e o contrato de resposta muda só nos 5 endpoints listados. Rollback é reverter o commit/branch — nenhum dado é migrado.
