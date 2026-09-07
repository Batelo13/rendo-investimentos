# Protecao CSRF Specification

## Purpose

Impede que um site externo force o navegador de um usuário autenticado a executar, sem seu conhecimento, uma requisição de escrita contra a aplicação (ataque CSRF), exigindo um token imprevisível por sessão em toda requisição que muda estado.

## Requirements

### Requirement: Token CSRF exigido em requisições de escrita
O sistema SHALL exigir um token CSRF válido, atrelado à sessão do usuário, em toda requisição HTTP que muda estado (POST, PUT, PATCH, DELETE), com exceção do console H2 (`/h2-console/**`).

#### Scenario: Requisição de escrita sem token é rejeitada
- **WHEN** uma requisição POST/PATCH/DELETE contra uma rota protegida chega sem um token CSRF válido
- **THEN** o sistema responde 403 Forbidden e não executa a operação

#### Scenario: Requisição de escrita com token válido é aceita
- **WHEN** uma requisição POST/PATCH/DELETE contra uma rota protegida chega com o token CSRF válido da sessão atual
- **THEN** o sistema processa a requisição normalmente

#### Scenario: Console H2 continua acessível sem token
- **WHEN** uma requisição de escrita é feita contra `/h2-console/**`
- **THEN** o sistema não exige token CSRF nessa rota

### Requirement: Páginas renderizadas pelo servidor expõem o token
Toda página HTML servida pelo backend que contém um formulário nativo ou dispara requisições de escrita via JavaScript SHALL expor o token CSRF da sessão atual, de forma que o navegador possa reenviá-lo.

#### Scenario: Formulário nativo carrega o token
- **WHEN** o usuário carrega uma página com um formulário nativo que faz POST (ex.: login, logout)
- **THEN** o HTML da página inclui o token CSRF da sessão atual embutido no próprio formulário

#### Scenario: Página com JavaScript expõe o token para leitura
- **WHEN** o usuário carrega uma página cujo JavaScript dispara requisições de escrita (ex.: cadastro, dashboard)
- **THEN** o HTML da página expõe o token CSRF e o nome do header esperado de forma legível pelo JavaScript da própria página
