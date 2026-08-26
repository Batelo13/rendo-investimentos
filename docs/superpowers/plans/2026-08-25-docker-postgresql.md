# Docker e PostgreSQL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Executar o Rendo e um PostgreSQL 17 com Docker Compose, preservando o perfil H2 atual e toda a lógica funcional da aplicação.

**Architecture:** Um `Dockerfile` multi-stage gera uma imagem Java 17 mínima e sem privilégios. O Compose coordena essa imagem com um PostgreSQL persistente e ativa um perfil Spring `docker` isolado; o perfil `dev` continua usando H2 sem alterações.

**Tech Stack:** Spring Boot 4.1.0, Java 17, Maven, Hibernate/JPA, PostgreSQL 17, Docker Desktop e Docker Compose.

---

## Estrutura de arquivos

- Criar `Dockerfile`: construir o JAR em uma etapa Maven e executá-lo em uma imagem JRE separada.
- Criar `.dockerignore`: impedir que artefatos, metadados e segredos entrem no contexto de build.
- Criar `compose.yaml`: coordenar PostgreSQL, aplicação, rede implícita, healthcheck e volume.
- Criar `src/main/resources/application-docker.properties`: configurar somente a execução PostgreSQL do container.
- Modificar `pom.xml`: adicionar o driver PostgreSQL em runtime.
- Modificar `.env.example`: documentar banco, portas e integrações opcionais sem valores reais.

### Task 1: Imagem Docker da aplicação

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: Executar a verificação negativa de arquivos ausentes**

Run:

```powershell
if ((Test-Path Dockerfile) -or (Test-Path .dockerignore)) {
    throw "Dockerfile ou .dockerignore ja existe"
}
throw "Arquivos Docker ainda ausentes, conforme esperado"
```

Expected: FAIL com `Arquivos Docker ainda ausentes, conforme esperado`.

- [ ] **Step 2: Criar o `Dockerfile` multi-stage**

```dockerfile
# syntax=docker/dockerfile:1

# Etapa 1: compila o projeto e gera o arquivo JAR.
FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copiar o pom antes do codigo permite reutilizar o cache das dependencias.
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: imagem final, contendo somente Java e a aplicacao compilada.
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# A aplicacao nao precisa executar como o usuario root do container.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: Criar o `.dockerignore`**

```dockerignore
# Controle de versao e configuracoes das IDEs
.git
.gitignore
.idea
.vscode
*.iml

# Configuracoes locais e segredos
.env
.env.*

# Artefatos de build e dependencias locais
target
build
*.class
*.log
node_modules
graphify-out

# Ferramentas locais que nao participam do build Maven no container
.mvn
mvnw
mvnw.cmd
package.json
package-lock.json
.claude
.grok
.worktrees
openspec

# Documentacao e arquivos do sistema
docs
HELP.md
README.md
.DS_Store
**/.DS_Store
```

- [ ] **Step 4: Validar a sintaxe e as práticas do Dockerfile**

Run:

```powershell
docker build --check .
```

Expected: exit code 0 e nenhuma violação impeditiva.

- [ ] **Step 5: Commit**

```powershell
git add Dockerfile .dockerignore
git commit -m "build: adiciona imagem Docker da aplicacao"
```

### Task 2: Perfil Spring para PostgreSQL

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/application-docker.properties`

- [ ] **Step 1: Executar a verificação negativa do perfil e do driver**

Run:

```powershell
$driverExiste = Select-String -Path pom.xml -Pattern '<artifactId>postgresql</artifactId>' -Quiet
$perfilExiste = Test-Path src/main/resources/application-docker.properties
if ($driverExiste -or $perfilExiste) {
    throw "Configuracao PostgreSQL ja existe"
}
throw "Driver e perfil Docker ainda ausentes, conforme esperado"
```

Expected: FAIL com `Driver e perfil Docker ainda ausentes, conforme esperado`.

- [ ] **Step 2: Adicionar o driver PostgreSQL ao `pom.xml`**

Inserir imediatamente depois da dependência `mysql-connector-j`, mantendo H2 e MySQL:

```xml
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
```

- [ ] **Step 3: Criar o perfil `application-docker.properties`**

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://postgres:5432/rendodb}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.h2.console.enabled=false

# Chaves de APIs externas - nunca hardcoded. Sobrescreva via variavel de ambiente.
brapi.api.token=${BRAPI_API_TOKEN:}
twelvedata.api.key=${TWELVEDATA_API_KEY:demo}
```

- [ ] **Step 4: Executar a suíte existente com o perfil `dev` inalterado**

Run:

```powershell
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`, todos os testes existentes aprovados e uso do H2 em memória.

- [ ] **Step 5: Empacotar o JAR**

Run:

```powershell
.\mvnw.cmd -DskipTests package
```

Expected: `BUILD SUCCESS` e `target/gestao-investimentos-0.0.1-SNAPSHOT.jar` criado.

- [ ] **Step 6: Commit**

```powershell
git add pom.xml src/main/resources/application-docker.properties
git commit -m "build: adiciona perfil PostgreSQL para Docker"
```

### Task 3: Orquestração com Docker Compose

**Files:**
- Create: `compose.yaml`
- Modify: `.env.example`

- [ ] **Step 1: Executar a verificação negativa do Compose ausente**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
docker compose config --quiet
```

Expected: FAIL informando que nenhum arquivo de configuração Compose foi encontrado.

- [ ] **Step 2: Criar o `compose.yaml`**

```yaml
name: gestao-investimentos

services:
  postgres:
    image: postgres:17-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-rendodb}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-rendodb}"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  aplicacao:
    build:
      context: .
      dockerfile: Dockerfile
    image: gestao-investimentos:local
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-rendodb}
      DB_USERNAME: ${POSTGRES_USER:-postgres}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      BRAPI_API_TOKEN: ${BRAPI_API_TOKEN:-}
      TWELVEDATA_API_KEY: ${TWELVEDATA_API_KEY:-demo}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}
      MICROSOFT_CLIENT_ID: ${MICROSOFT_CLIENT_ID:-}
      MICROSOFT_CLIENT_SECRET: ${MICROSOFT_CLIENT_SECRET:-}
      MICROSOFT_TENANT_ID: ${MICROSOFT_TENANT_ID:-common}
    ports:
      - "${APP_PORT:-8080}:8080"

volumes:
  postgres_data:
```

- [ ] **Step 3: Atualizar o `.env.example` completo**

```dotenv
# Variaveis utilizadas pelo Docker Compose
POSTGRES_DB=rendodb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=troque-esta-senha
POSTGRES_PORT=5432
APP_PORT=8080

# APIs externas opcionais
BRAPI_API_TOKEN=
TWELVEDATA_API_KEY=demo

# Login social opcional
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
MICROSOFT_CLIENT_ID=
MICROSOFT_CLIENT_SECRET=
MICROSOFT_TENANT_ID=common
```

- [ ] **Step 4: Validar o Compose sem exibir valores resolvidos**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
docker compose config --quiet
docker compose config --services
```

Expected: exit code 0 e somente os serviços `postgres` e `aplicacao` listados.

- [ ] **Step 5: Confirmar que nenhum segredo foi versionado**

Run:

```powershell
git status --short
git check-ignore -v .env
git ls-files .env
```

Expected: `.env` aparece como ignorado e `git ls-files .env` não retorna conteúdo.

- [ ] **Step 6: Commit**

```powershell
git add compose.yaml .env.example
git commit -m "build: orquestra aplicacao e PostgreSQL"
```

### Task 4: Verificação integrada e limpeza segura

**Files:**
- Verify: `Dockerfile`
- Verify: `.dockerignore`
- Verify: `compose.yaml`
- Verify: `pom.xml`
- Verify: `src/main/resources/application-docker.properties`
- Verify: `.env.example`

- [ ] **Step 1: Executar testes e empacotamento do zero lógico**

Run:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Expected: os dois comandos terminam com `BUILD SUCCESS`.

- [ ] **Step 2: Construir as imagens com credenciais temporárias de verificação**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
$env:POSTGRES_PORT = "15432"
$env:APP_PORT = "18080"
docker compose build
```

Expected: imagem `gestao-investimentos:local` construída com sucesso.

- [ ] **Step 3: Iniciar os serviços**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
$env:POSTGRES_PORT = "15432"
$env:APP_PORT = "18080"
docker compose up -d
docker compose ps
```

Expected: `postgres` fica `healthy` e `aplicacao` permanece em execução.

- [ ] **Step 4: Verificar perfil, banco e resposta HTTP**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
$env:POSTGRES_PORT = "15432"
$env:APP_PORT = "18080"
docker compose logs --no-color aplicacao
$response = Invoke-WebRequest -UseBasicParsing http://localhost:18080/login
$response.StatusCode
```

Expected: logs mostram o perfil `docker`, conexão PostgreSQL sem erro e status HTTP `200`.

- [ ] **Step 5: Confirmar preservação do volume ao encerrar normalmente**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
$env:POSTGRES_PORT = "15432"
$env:APP_PORT = "18080"
docker compose down
docker volume ls --filter name=gestao-investimentos_postgres_data
```

Expected: containers e rede removidos; volume `gestao-investimentos_postgres_data` permanece listado.

- [ ] **Step 6: Remover somente o volume temporário criado por esta verificação**

Run:

```powershell
$env:POSTGRES_PASSWORD = "rendo-local-test"
$env:POSTGRES_PORT = "15432"
$env:APP_PORT = "18080"
docker compose down --volumes
```

Expected: o volume de teste `gestao-investimentos_postgres_data`, criado no Step 3 e sem dados do usuário, é removido para não conflitar com a senha real escolhida depois.

- [ ] **Step 7: Revisar o diff e o estado final**

Run:

```powershell
git diff --check
git status --short
```

Expected: nenhum erro de whitespace e somente alterações Docker previstas, caso os commits tenham ficado a cargo do usuário.
