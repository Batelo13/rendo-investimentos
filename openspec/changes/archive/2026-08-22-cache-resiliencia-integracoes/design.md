## Context

Os 5 clients de API externa já usam `RestClient` com timeout de 5s e traduzem toda falha em `RecursoNaoEncontradoException` (4xx/dado inválido) ou `ServicoExternoIndisponivelException` (rede/5xx/config). Ver proposal.md - Why. Nenhum retry, cache ou circuit breaker existia antes desta change.

## Goals / Non-Goals

**Goals:**
- Tolerar falha transiente de rede sem propagar erro na primeira tentativa.
- Parar de bater numa API externa consistentemente fora do ar, em vez de esperar o timeout completo a cada requisição nova.
- Evitar chamada repetida à BrasilAPI/ViaCEP para o mesmo CNPJ/CEP.

**Non-Goals:**
- Cache de cotação (câmbio, ações) — cotação muda com frequência, cache aqui seria dado desatualizado, não otimização.
- TTL/eviction no cache de CNPJ/CEP — nesta primeira versão o cache vive pela duração do processo (JVM), sem expiração. Se um CNPJ mudar razão social/endereço, só se reflete após reiniciar a aplicação — aceitável para o escopo acadêmico atual.
- Retry/circuit breaker configurável por ambiente (dev/prod) — mesma configuração para todos os profiles nesta primeira versão.

## Decisions

- **Retry hand-rolled em vez de Spring Retry**: `RetryExterno.tentar(...)` é um helper de ~20 linhas usado pelos 5 clients, sem dependência nova. Alternativa considerada (`spring-retry` + `@Retryable`, que exigiria também AOP) foi descartada por trazer 2 dependências novas para uma lógica simples o suficiente para não precisar de biblioteca.
- **Retry só em falha transiente**: distingue `ResourceAccessException`/`HttpServerErrorException` (repete) de `HttpClientErrorException` 4xx (nunca repete) — repetir um erro de negócio (CNPJ inválido) não vira válido tentando de novo, só atrasaria a resposta de erro.
- **Circuit breaker via Resilience4j**: ao contrário do retry, reimplementar a máquina de estados de um circuit breaker (closed/open/half-open) corretamente sob concorrência é fácil de errar — é exatamente o tipo de problema que essa biblioteca resolve bem. `resilience4j-spring-boot3` usada mesmo em Spring Boot 4.1.0 porque a API de anotações (`@CircuitBreaker`) não depende de nenhum recurso específico do módulo `spring-boot-starter-web` que foi reorganizado no Boot 4.
- **`aspectjweaver` direto em vez de `spring-boot-starter-aop`**: Spring Boot 4.1.0 não gerencia mais esse starter (não existe na sua BOM). O que os `@Aspect` do Resilience4j precisam é só o `aspectjweaver` no classpath para o autoproxy do Spring reconhecer as anotações de pointcut — não é weaving real (load-time), então a dependência sozinha basta, sem plugin de build adicional.
- **Cache indefinido (sem TTL) via `spring-boot-starter-cache`**: usa `ConcurrentMapCacheManager` (auto-configurado pelo Spring Boot quando nenhum provedor externo como Redis/Caffeine está no classpath) — cache em memória do próprio processo, sem infraestrutura extra. Suficiente para o volume de dados de um projeto acadêmico.
- **`RecursoNaoEncontradoException` ignorada pelo circuit breaker**: configurado via `resilience4j.circuitbreaker.configs.default.ignore-exceptions`. Sem isso, um CNPJ inválido digitado repetidamente por um usuário abriria o circuito para todo mundo, mesmo com a API externa saudável.

## Risks / Trade-offs

- [Cache sem TTL pode servir dado desatualizado se o CNPJ/CEP mudar de fato (raro, mas possível)] → aceito conscientemente para o escopo atual; upgrade path: `@Cacheable` com `Caffeine` + TTL se isso virar um problema real.
- [`RetryExterno` duplica uma responsabilidade que uma biblioteca padrão (Spring Retry) resolveria de forma mais declarativa] → aceito porque a lógica é pequena e evita 2 dependências novas; se o número de padrões de retry crescer (backoff exponencial, retry condicional por tipo de exceção mais granular), reavaliar Spring Retry.
- [Resilience4j é uma dependência nova, específica, testada aqui pela primeira vez contra Spring Boot 4.1.0] → mitigado por rodar a suíte de testes completa e verificação manual no navegador antes de mesclar.

## Migration Plan

Mudança é aditiva na camada de integração; nenhum dado migrado, nenhuma mudança de schema. Rollback é reverter o commit — os 5 clients voltam ao comportamento anterior (sem cache/retry/circuit breaker), sem efeito colateral em dados já persistidos.
