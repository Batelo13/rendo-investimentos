## 1. Dependências e configuração

- [x] 1.1 Adicionar `spring-boot-starter-cache`, `io.github.resilience4j:resilience4j-spring-boot3` e `org.aspectj:aspectjweaver` ao `pom.xml`
- [x] 1.2 `@EnableCaching` em `GestaoInvestimentosApplication`
- [x] 1.3 Configurar circuit breaker (sliding window, threshold, wait duration, `ignore-exceptions`) em `application.properties`, com uma instância nomeada por client (`cnpj`, `cep`, `cambio`, `cotacaoEua`, `cotacaoBrasil`)

## 2. Retry

- [x] 2.1 Criar `RetryExterno.tentar(tentativas, atrasoMs, acao)` — repete só em `ResourceAccessException`/`HttpServerErrorException`, nunca em erro 4xx
- [x] 2.2 Envolver a chamada HTTP dos 5 clients (`BrasilApiCnpjClient`, `ViaCepClient`, `TwelveDataCambioClient`, `TwelveDataCotacaoProvider`, `BrapiCotacaoProvider`) com `RetryExterno.tentar(3, 300, ...)`

## 3. Circuit breaker

- [x] 3.1 `@CircuitBreaker` + `fallbackMethod` (lança `ServicoExternoIndisponivelException`) nos 5 clients

## 4. Cache

- [x] 4.1 `@Cacheable("cnpj")` em `BrasilApiCnpjClient.buscar()`
- [x] 4.2 `@Cacheable("cep")` em `ViaCepClient.buscar()`

## 5. Verificação

- [x] 5.1 `mvnw compile` e `mvnw test` passam com as novas dependências
- [x] 5.2 Verificação manual no navegador: cadastro de corretora real (CNPJ da XP Investimentos, CEP resolvido, validação CVM ok) e cadastro de ação real BR (PETR4, brapi) e EUA (AAPL, Twelve Data + conversão de câmbio) — todos funcionando com os clients decorados por cache/retry/circuit breaker
- [x] 5.3 Log do servidor sem erro de proxy AOP ou bean não encontrado (só um warning pré-existente de paginação, não relacionado a esta change); console do navegador sem erro do app (só ruído de extensão do Chrome)
