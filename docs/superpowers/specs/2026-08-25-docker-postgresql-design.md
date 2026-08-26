# Docker e PostgreSQL — Especificação de Design

## Objetivo

Containerizar o projeto Rendo seguindo o padrão didático da Aula 12 do professor: imagem Spring Boot construída em múltiplas etapas, aplicação e PostgreSQL coordenados pelo Docker Compose, banco persistido em volume e inicialização da aplicação condicionada à saúde do banco. A mudança não altera regras de negócio, controladores, serviços, entidades, páginas ou testes funcionais existentes.

Referência: <https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-12-DOCKER-E-CONTAINERS.md>

## Estado atual

- Spring Boot 4.1.0, Maven e Java 17.
- O perfil padrão `dev` usa H2 em memória e `spring.jpa.hibernate.ddl-auto=create-drop`.
- O projeto possui os drivers H2 e MySQL, mas não possui configuração MySQL ativa.
- O projeto não usa Liquibase ou Flyway.
- O arquivo `.env` local contém credenciais de integrações e já está ignorado pelo Git.
- Não existem `Dockerfile`, `.dockerignore` ou arquivo Compose.

## Abordagem aprovada

O perfil `dev` e o H2 permanecem inalterados. Um perfil `docker` isolado passa a usar PostgreSQL 17. Essa separação preserva a experiência atual na IDE e reproduz no Docker a arquitetura da aula sem converter todo o desenvolvimento local para PostgreSQL.

O driver PostgreSQL será adicionado como dependência de runtime. O driver MySQL será mantido para evitar uma remoção fora do escopo.

## Arquivos e responsabilidades

### `Dockerfile`

- Usa `maven:3.9.11-eclipse-temurin-17` para resolver dependências e gerar o JAR.
- Copia primeiro o `pom.xml` para aproveitar o cache de dependências.
- Usa `eclipse-temurin:17-jre-jammy` na imagem final.
- Copia apenas o JAR para a imagem final.
- Executa a aplicação com um usuário `spring` sem privilégios administrativos.
- Documenta a porta interna 8080.

### `.dockerignore`

Exclui metadados Git, configurações de IDE, artefatos de build, dependências Node, documentação, worktrees, arquivos locais e qualquer `.env` do contexto enviado ao Docker. O código-fonte, `pom.xml` e recursos necessários ao build continuam disponíveis.

### `compose.yaml`

Declara dois serviços:

- `postgres`: usa `postgres:17-alpine`, recebe banco, usuário e senha via variáveis, publica uma porta configurável, persiste dados em `postgres_data` e usa `pg_isready` no healthcheck;
- `aplicacao`: constrói o `Dockerfile`, ativa o perfil `docker`, conecta-se a `postgres:5432`, aguarda o banco saudável e publica a porta 8080 por uma porta configurável no computador.

As credenciais opcionais das APIs externas e do login social são repassadas ao container da aplicação. Valores reais não entram no Dockerfile, na imagem ou no Git.

## Configuração Spring

O novo `src/main/resources/application-docker.properties` define:

- URL JDBC, usuário e senha por `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`;
- driver `org.postgresql.Driver`;
- `spring.jpa.hibernate.ddl-auto=update`, para criar e evoluir o esquema didático e preservar os dados no volume;
- console H2 desabilitado no container.

O uso de `update` é uma adaptação necessária em relação à aula: o projeto do professor usa Liquibase e validação de esquema, enquanto o Rendo não possui migrações. Adicionar Liquibase exigiria desenhar e validar um histórico de schema, o que ampliaria o escopo e poderia alterar o comportamento funcional.

## Variáveis locais

O `.env.example` documentará:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `POSTGRES_PORT`;
- `APP_PORT`;
- as variáveis existentes de BRAPI e Twelve Data;
- as variáveis opcionais de Google e Microsoft já aceitas pela aplicação.

O `.env` real não será sobrescrito. Antes da primeira execução, o usuário deve acrescentar nele as variáveis de banco documentadas no exemplo e escolher uma senha local.

## Fluxo de inicialização

1. O Compose cria a rede e o volume `postgres_data`.
2. O PostgreSQL inicializa o banco e passa pelo healthcheck.
3. O serviço `aplicacao` inicia com o perfil `docker`.
4. O Spring conecta-se ao hostname interno `postgres`.
5. O Hibernate atualiza o esquema no banco persistente.
6. O Tomcat atende na porta 8080 interna, publicada como `APP_PORT` no computador.

## Falhas e diagnóstico

- Senha ausente ou incorreta impede a inicialização saudável do PostgreSQL ou a conexão do Spring; os logs dos dois serviços mostram a causa.
- Porta ocupada é resolvida alterando `APP_PORT` ou `POSTGRES_PORT`, sem modificar código Java.
- A aplicação não usa `localhost` para acessar o banco dentro do Compose; usa o nome de serviço `postgres`.
- `docker compose down` remove containers e rede, preservando os dados.
- `docker compose down -v` também apaga o volume e deve ser usado somente quando a intenção for recriar o banco.

## Verificação

A entrega será verificada por:

1. testes Maven existentes;
2. empacotamento Maven;
3. `docker compose config` e listagem dos serviços;
4. build das imagens;
5. inicialização dos dois serviços;
6. estado saudável do PostgreSQL e aplicação em execução;
7. resposta HTTP da tela de login na porta publicada;
8. inspeção dos logs da aplicação para confirmar o perfil `docker` e a conexão PostgreSQL;
9. encerramento com preservação do volume.

## Fora de escopo

- Alterar lógica funcional ou páginas da aplicação.
- Migrar o perfil `dev` de H2 para PostgreSQL.
- Remover o suporte MySQL existente.
- Introduzir Liquibase ou Flyway.
- Colocar credenciais reais em arquivos versionados.
- Configurar publicação em registry ou ambiente de produção.
