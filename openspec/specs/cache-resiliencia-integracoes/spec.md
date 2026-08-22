## Purpose

Torna as integrações com APIs externas (CNPJ, CEP, câmbio, cotação de ações BR/EUA) tolerantes a falha transiente e reduz chamadas repetidas para dados que praticamente não mudam.

## Requirements

### Requirement: Retry em falha transiente nas chamadas externas
O sistema SHALL repetir automaticamente uma chamada a uma API externa até 3 vezes quando a falha for de rede (timeout, conexão recusada) ou erro 5xx do servidor remoto, antes de reportar indisponibilidade.

#### Scenario: Falha de rede pontual se recupera na segunda tentativa
- **WHEN** a primeira tentativa de consulta a uma API externa falha por timeout, mas a segunda tentativa é bem-sucedida
- **THEN** o sistema retorna o resultado da segunda tentativa normalmente, sem expor a falha da primeira ao usuário

#### Scenario: Erro de negócio não é repetido
- **WHEN** uma consulta de CNPJ, CEP ou ticker retorna erro 4xx (recurso inválido ou inexistente)
- **THEN** o sistema não tenta novamente e reporta o erro de "não encontrado" imediatamente

### Requirement: Circuit breaker por integração externa
O sistema SHALL parar de tentar uma API externa por um período quando a taxa de falhas recentes for consistentemente alta, retornando indisponibilidade imediatamente em vez de esperar o timeout de cada nova requisição.

#### Scenario: Circuito abre após falhas consecutivas
- **WHEN** uma API externa falha repetidamente acima do limiar configurado
- **THEN** novas requisições àquela API passam a falhar imediatamente com indisponibilidade de serviço, sem nova tentativa de rede, até o circuito ser reavaliado

#### Scenario: "Não encontrado" não conta como falha de infraestrutura
- **WHEN** uma consulta retorna "não encontrado" (CNPJ/CEP/ticker inválido)
- **THEN** essa resposta não é contabilizada como falha para fins de abertura do circuito, pois é um resultado de negócio válido, não uma indisponibilidade da API

### Requirement: Cache de consulta de CNPJ e CEP
O sistema SHALL armazenar em cache o resultado de uma consulta de CNPJ ou CEP bem-sucedida, evitando uma nova chamada à API externa para o mesmo valor.

#### Scenario: Segunda consulta ao mesmo CNPJ não chama a API externa novamente
- **WHEN** o mesmo CNPJ é consultado duas vezes
- **THEN** a segunda consulta retorna o resultado em cache, sem nova chamada à BrasilAPI

#### Scenario: Consulta com falha não é armazenada em cache
- **WHEN** uma consulta de CNPJ ou CEP falha (não encontrado ou indisponibilidade)
- **THEN** o resultado da falha não é armazenado em cache, permitindo que uma nova tentativa futura chame a API externa normalmente
