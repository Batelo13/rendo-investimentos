## Why

Os 5 clients de API externa (`BrasilApiCnpjClient`, `ViaCepClient`, `TwelveDataCambioClient`, `TwelveDataCotacaoProvider`, `BrapiCotacaoProvider`) já tinham timeout e tradução de erro em exceção tipada, mas nenhuma resiliência a falha transiente: uma falha de rede pontual falha na hora, sem tentar de novo; CNPJ/CEP (dados praticamente estáticos) são buscados de novo a cada requisição; e se uma API externa cair, cada nova requisição ainda espera o timeout inteiro em vez de desistir rápido.

## What Changes

- **Cache de CNPJ/CEP**: `BrasilApiCnpjClient.buscar()` e `ViaCepClient.buscar()` ganham `@Cacheable` (Spring Cache, `spring-boot-starter-cache`, cache em memória via `ConcurrentMapCacheManager`) — resultado fica em cache indefinidamente pela vida do processo (sem TTL/eviction nesta primeira versão).
- **Retry em falha transiente**: novo helper `RetryExterno.tentar(tentativas, atrasoMs, acao)` usado pelos 5 clients, repetindo a chamada até 3 vezes (300ms de intervalo) apenas quando a falha é transiente (`ResourceAccessException` — timeout/conexão recusada — ou `HttpServerErrorException` — 5xx). Erro 4xx (CNPJ/ticker inválido) nunca é repetido, pois é permanente.
- **Circuit breaker**: `@CircuitBreaker` (Resilience4j) em cada um dos 5 clients, com um `fallbackMethod` que lança `ServicoExternoIndisponivelException` quando o circuito está aberto. `RecursoNaoEncontradoException` é ignorada na contagem de falhas do circuito — um "não encontrado" é resultado de negócio, não indisponibilidade de infraestrutura.
- Novas dependências: `spring-boot-starter-cache`, `io.github.resilience4j:resilience4j-spring-boot3`, `org.aspectj:aspectjweaver` (Spring Boot 4.1.0 não gerencia mais um starter de AOP dedicado — `aspectjweaver` sozinho já cobre o que os `@Aspect` do Resilience4j precisam).

## Capabilities

### New Capabilities
- `cache-resiliencia-integracoes`: exige que chamadas às APIs externas (CNPJ, CEP, câmbio, cotação BR/EUA) tolerem falha transiente via retry, parem de tentar quando a API está consistentemente fora do ar via circuit breaker, e cache CNPJ/CEP evite chamadas repetidas para dados que praticamente não mudam.

### Modified Capabilities
(nenhuma — nenhum contrato de API pública muda; a mudança é só na camada de integração)

## Impact

- `integration/BrasilApiCnpjClient.java`, `integration/ViaCepClient.java`, `integration/TwelveDataCambioClient.java`, `integration/TwelveDataCotacaoProvider.java`, `integration/BrapiCotacaoProvider.java`: retry + circuit breaker; os dois primeiros também ganham `@Cacheable`.
- `integration/RetryExterno.java` (novo, package-private).
- `GestaoInvestimentosApplication.java`: `@EnableCaching`.
- `application.properties`: configuração do circuit breaker (sliding window, threshold, wait duration, exceções ignoradas).
- `pom.xml`: 3 novas dependências.
