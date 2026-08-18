# Cadastro de CPF do Usuário Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar um campo `cpf` obrigatório e único ao cadastro de `Usuario`, validado por formato + dígito verificador (sem lookup externo), exposto na API e no formulário de cadastro.

**Architecture:** Nova anotação Bean Validation `@CPF` (pacote `validation/`) valida o dígito verificador no `UsuarioRequestDTO`, mesmo nível de camada que `@Email`/`@Pattern` já usam. `Usuario`/`UsuarioRepository`/`UsuarioService`/`UsuarioResponseDTO` seguem exatamente o padrão já usado por `email` (campo único, checagem de duplicidade antes de salvar, exposto na resposta). Frontend replica o padrão já usado pelo campo CNPJ no dashboard: input livre, dígitos extraídos no submit, validação client-side só de comprimento.

**Tech Stack:** Spring Boot (Jakarta Bean Validation), JPA/Hibernate, JUnit 5, Thymeleaf + JS puro (sem libs novas).

Spec de referência: `docs/superpowers/specs/2026-08-18-cadastro-cpf-design.md`.

---

### Task 1: `@CPF` + `CpfValidator`

**Files:**
- Create: `src/main/java/com/curso/gestaoinvestimentos/validation/CPF.java`
- Create: `src/main/java/com/curso/gestaoinvestimentos/validation/CpfValidator.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/validation/CpfValidatorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.curso.gestaoinvestimentos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void aceitaCpfValidoComDigitosVerificadoresCorretos() {
        assertTrue(validator.isValid("11144477735", null));
        assertTrue(validator.isValid("12345678909", null));
    }

    @Test
    void rejeitaCpfComDigitoVerificadorErrado() {
        assertFalse(validator.isValid("11144477736", null));
    }

    @Test
    void rejeitaCpfComTodosOsDigitosIguais() {
        assertFalse(validator.isValid("11111111111", null));
        assertFalse(validator.isValid("00000000000", null));
    }

    @Test
    void rejeitaCpfComTamanhoErradoOuComPontuacao() {
        assertFalse(validator.isValid("111444777", null));
        assertFalse(validator.isValid("111.444.777-35", null));
    }

    @Test
    void aceitaNuloOuVazioDeixandoNotBlankCuidarDaObrigatoriedade() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test "-Dtest=CpfValidatorTest"`
Expected: FAIL to compile — `CpfValidator` does not exist yet.

- [ ] **Step 3: Write the annotation**

```java
package com.curso.gestaoinvestimentos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface CPF {

    String message() default "CPF invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

- [ ] **Step 4: Write the validator**

```java
package com.curso.gestaoinvestimentos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }
        if (!cpf.matches("\\d{11}") || todosOsDigitosIguais(cpf)) {
            return false;
        }

        int[] digitos = new int[11];
        for (int i = 0; i < 11; i++) {
            digitos[i] = cpf.charAt(i) - '0';
        }

        int dv1 = calcularDigitoVerificador(digitos, 9);
        int dv2 = calcularDigitoVerificador(digitos, 10);
        return digitos[9] == dv1 && digitos[10] == dv2;
    }

    private boolean todosOsDigitosIguais(String cpf) {
        return cpf.chars().distinct().count() == 1;
    }

    private int calcularDigitoVerificador(int[] digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\mvnw.cmd test "-Dtest=CpfValidatorTest"`
Expected: PASS — 5 testes verdes.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/validation/CPF.java src/main/java/com/curso/gestaoinvestimentos/validation/CpfValidator.java src/test/java/com/curso/gestaoinvestimentos/validation/CpfValidatorTest.java
git commit -m "feat(usuario): valida cpf via digito verificador (@CPF)"
```

---

### Task 2: `CpfTestFixtures` (gerador de CPF válido único para testes de integração)

**Files:**
- Create: `src/test/java/com/curso/gestaoinvestimentos/util/CpfTestFixtures.java`

Sem passo de teste dedicado: não é código de produção, e sua corretude é a mesma fórmula já
coberta pelo `CpfValidatorTest` (Task 1) — os testes que a usam (Tasks 8 e 9) validam
indiretamente que ela gera CPFs aceitos pelo backend.

- [ ] **Step 1: Create the fixture**

```java
package com.curso.gestaoinvestimentos.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gera CPFs sinteticos (nao pertencem a nenhuma pessoa real) com digito
 * verificador correto, um por chamada, para testes que precisam de um
 * Usuario persistido (cpf e unico no banco).
 */
public class CpfTestFixtures {

    private static final AtomicInteger CONTADOR = new AtomicInteger(100_000_000);

    private CpfTestFixtures() {
    }

    public static String proximoCpfValido() {
        return gerarCpfValido(CONTADOR.getAndIncrement());
    }

    private static String gerarCpfValido(int semente) {
        String base = String.format("%09d", semente % 1_000_000_000);
        int[] digitos = new int[11];
        for (int i = 0; i < 9; i++) {
            digitos[i] = base.charAt(i) - '0';
        }
        digitos[9] = calcularDigitoVerificador(digitos, 9);
        digitos[10] = calcularDigitoVerificador(digitos, 10);

        StringBuilder sb = new StringBuilder();
        for (int digito : digitos) {
            sb.append(digito);
        }
        return sb.toString();
    }

    private static int calcularDigitoVerificador(int[] digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/util/CpfTestFixtures.java
git commit -m "test(usuario): fixture de geracao de cpf sintetico valido"
```

---

### Task 3: Campo `cpf` em `Usuario`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/model/Usuario.java`

- [ ] **Step 1: Add the field**

Em `src/main/java/com/curso/gestaoinvestimentos/model/Usuario.java:25-29`, entre `email` e
`senha`:

```java
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String senha;
```

- [ ] **Step 2: Add the getter/setter**

Em `src/main/java/com/curso/gestaoinvestimentos/model/Usuario.java`, logo após `setEmail`
(linha 61 atual):

```java
    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
```

- [ ] **Step 3: Compile to confirm no syntax errors**

Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS (nada mais usa `Usuario.cpf` ainda, então não quebra nada).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/model/Usuario.java
git commit -m "feat(usuario): adiciona campo cpf ao modelo"
```

---

### Task 4: `UsuarioRepository.findByCpf`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/repository/UsuarioRepository.java`

- [ ] **Step 1: Add the method**

```java
package com.curso.gestaoinvestimentos.repository;

import com.curso.gestaoinvestimentos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);
}
```

- [ ] **Step 2: Compile to confirm no syntax errors**

Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/repository/UsuarioRepository.java
git commit -m "feat(usuario): adiciona busca por cpf no repository"
```

---

### Task 5: Campo `cpf` em `UsuarioRequestDTO`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/dto/UsuarioRequestDTO.java`

Este passo quebra a compilação de `UsuarioAuthIntegrationTest.java:72` (chamada posicional de
`new UsuarioRequestDTO(...)` com 3 argumentos) — corrigido na Task 8. Normal ficar
temporariamente vermelho entre esta task e a Task 8.

- [ ] **Step 1: Add the field**

```java
package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.validation.CPF;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(

        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 digitos numericos, sem pontuacao")
        @CPF(message = "CPF invalido")
        String cpf,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
        String senha
) {
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/dto/UsuarioRequestDTO.java
git commit -m "feat(usuario): exige cpf valido no cadastro"
```

---

### Task 6: Campo `cpf` em `UsuarioResponseDTO`

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/dto/UsuarioResponseDTO.java`

- [ ] **Step 1: Add the field**

```java
package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Role;

import java.time.LocalDate;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cpf,
        Role role,
        Boolean ativo,
        LocalDate dataCadastro
) {
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/dto/UsuarioResponseDTO.java
git commit -m "feat(usuario): expoe cpf na resposta da api"
```

---

### Task 7: `UsuarioService` — checagem de duplicidade + mapeamento

**Files:**
- Modify: `src/main/java/com/curso/gestaoinvestimentos/service/UsuarioService.java:39-64`

- [ ] **Step 1: Update `cadastrar()`**

Substituir o método inteiro (linhas 39-64):

```java
    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        repository.findByEmail(dto.email()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email " + dto.email());
        });
        repository.findByCpf(dto.cpf()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o CPF " + dto.cpf());
        });

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCpf(dto.cpf());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        // Campos controlados pelo sistema, nunca pelo cliente:
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());

        Usuario salvo = repository.save(usuario);

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(SALDO_INICIAL_PADRAO);
        carteiraRepository.save(carteira);

        return toResponseDTO(salvo);
    }
```

- [ ] **Step 2: Update `toResponseDTO()`**

Substituir o método (linhas 100-109 atuais):

```java
    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getDataCadastro()
        );
    }
```

- [ ] **Step 3: Compile to confirm no syntax errors**

Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/curso/gestaoinvestimentos/service/UsuarioService.java
git commit -m "feat(usuario): valida cpf duplicado no cadastro"
```

---

### Task 8: Atualizar `UsuarioAuthIntegrationTest`

**Files:**
- Modify: `src/test/java/com/curso/gestaoinvestimentos/UsuarioAuthIntegrationTest.java`

- [ ] **Step 1: Add the import**

Em `UsuarioAuthIntegrationTest.java:7`, logo após o import de `Usuario`:

```java
import com.curso.gestaoinvestimentos.repository.CarteiraRepository;
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
```

- [ ] **Step 2: Update the `cadastrarUsuario` helper**

Substituir o método (linhas 59-68 atuais):

```java
    private Usuario cadastrarUsuario(String email, String senhaPlana, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setRole(role);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());
        return usuarioRepository.save(usuario);
    }
```

- [ ] **Step 3: Fix the existing cadastro test and assert the cpf round-trips**

Substituir o teste (linhas 70-83 atuais):

```java
    @Test
    void deveCadastrarUsuarioPublicamenteSemAutenticacao() throws Exception {
        String cpf = CpfTestFixtures.proximoCpfValido();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Maria Silva", "maria@example.com", cpf, "senha1234");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void deveRejeitarCadastroComCpfInvalido() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "Joao Invalido", "joao.invalido@example.com", "11144477736", "senha1234");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarCadastroComCpfDuplicado() throws Exception {
        String cpf = CpfTestFixtures.proximoCpfValido();
        UsuarioRequestDTO primeiro = new UsuarioRequestDTO(
                "Primeiro Usuario", "primeiro.cpf@example.com", cpf, "senha1234");
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeiro)))
                .andExpect(status().isCreated());

        UsuarioRequestDTO segundo = new UsuarioRequestDTO(
                "Segundo Usuario", "segundo.cpf@example.com", cpf, "senha1234");
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(segundo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe um usuario cadastrado com o CPF " + cpf));
    }
```

- [ ] **Step 4: Run the full test class**

Run: `.\mvnw.cmd test "-Dtest=UsuarioAuthIntegrationTest"`
Expected: PASS — todos os testes (os pré-existentes + os 2 novos) verdes.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/UsuarioAuthIntegrationTest.java
git commit -m "test(usuario): cobre cpf invalido, duplicado e no cadastro publico"
```

---

### Task 9: Atualizar `OperacaoIntegrationTest`

**Files:**
- Modify: `src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java`

Este arquivo tem seu próprio helper `cadastrarUsuario` (não reaproveita o de
`UsuarioAuthIntegrationTest`) e cria `Usuario` direto pelo repository — precisa do mesmo ajuste
de `cpf` só para continuar compilando e persistindo (nenhum teste novo aqui, o cpf não é
relevante para o domínio de operações).

- [ ] **Step 1: Add the import**

Em `OperacaoIntegrationTest.java:19`, logo após o import de `UsuarioRepository`:

```java
import com.curso.gestaoinvestimentos.repository.UsuarioRepository;
import com.curso.gestaoinvestimentos.util.CpfTestFixtures;
```

- [ ] **Step 2: Update the `cadastrarUsuario` helper**

Substituir o método (linhas 77-94 atuais):

```java
    private Usuario cadastrarUsuario(String email, String senhaPlana, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste");
        usuario.setEmail(email);
        usuario.setCpf(CpfTestFixtures.proximoCpfValido());
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setRole(role);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDate.now());
        Usuario salvo = usuarioRepository.save(usuario);

        Carteira carteira = new Carteira();
        carteira.setUsuario(salvo);
        carteira.setDataCriacao(LocalDate.now());
        carteira.setSaldoInicial(SALDO_INICIAL_TESTE);
        carteiraRepository.save(carteira);

        return salvo;
    }
```

- [ ] **Step 3: Run the full test class**

Run: `.\mvnw.cmd test "-Dtest=OperacaoIntegrationTest"`
Expected: PASS — todos os cenários já existentes continuam verdes (regressão de graça, cpf não
afeta nenhuma regra de negócio de operação).

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/curso/gestaoinvestimentos/OperacaoIntegrationTest.java
git commit -m "test(operacao): ajusta fixture de usuario para incluir cpf"
```

---

### Task 10: Rodar a suíte completa antes do frontend

- [ ] **Step 1: Run every test**

Run: `.\mvnw.cmd test`
Expected: BUILD SUCCESS, 0 failures. Se algum outro teste (fora dos dois arquivos já ajustados)
falhar por causa do `cpf` obrigatório, é sinal de mais um lugar que cria `Usuario` direto — volte
e aplique o mesmo ajuste do Step 2 das Tasks 8/9 nele antes de prosseguir.

---

### Task 11: Campo CPF no formulário de cadastro (`login.html`)

**Files:**
- Modify: `src/main/resources/templates/login.html:34-45`

- [ ] **Step 1: Add the input**

Substituir o bloco de inputs do formulário de cadastro (linhas 34-45 atuais) por:

```html
                <div class="input-group">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21a8 8 0 0 0-16 0"/><circle cx="12" cy="7" r="4"/></svg>
                    <input type="text" name="nome" placeholder="Nome" required>
                </div>
                <div class="input-group">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 6-10 7L2 6"/></svg>
                    <input type="email" name="email" placeholder="Email" required>
                </div>
                <div class="input-group">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h.01M7 12h.01M11 8h6M11 12h6M7 16h10"/></svg>
                    <input type="text" name="cpf" placeholder="CPF" inputmode="numeric" maxlength="14" required>
                </div>
                <div class="input-group">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>
                    <input type="password" name="senha" placeholder="Senha (mínimo 8 caracteres)" minlength="8" required>
                </div>
```

`maxlength="14"` cobre o caso do usuário digitar com pontuação (`000.000.000-00` tem 14
caracteres) — o JS extrai só os dígitos no submit (Task 12), então tanto digitar formatado
quanto só números funciona.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "feat(frontend): adiciona campo cpf ao formulario de cadastro"
```

---

### Task 12: Enviar e validar o CPF em `login.js`

**Files:**
- Modify: `src/main/resources/static/js/login.js`

- [ ] **Step 1: Add the `onlyDigits` helper and use it in the submit DTO**

Em `login.js:24-28`, substituir o objeto `dto`:

```javascript
const onlyDigits = (s) => (s || '').replace(/\D/g, '');

cadastroForm.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    cadastroMensagem.textContent = '';
    cadastroMensagem.className = 'form-mensagem';

    const dto = {
        nome: cadastroForm.nome.value,
        email: cadastroForm.email.value,
        cpf: onlyDigits(cadastroForm.cpf.value),
        senha: cadastroForm.senha.value,
    };
```

(A declaração de `onlyDigits` fica antes do listener existente, no mesmo escopo de módulo — não
precisa mover mais nada, só inserir a linha e trocar o corpo do `dto`.)

- [ ] **Step 2: Add the live validator**

Em `login.js:65-70`, adicionar `cpf` ao objeto `validadores`:

```javascript
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const validadores = {
    nome: (v) => v.trim().length >= 2,
    email: (v) => EMAIL_REGEX.test(v.trim()),
    cpf: (v) => onlyDigits(v).length === 11,
    senha: (v) => v.length >= 8,
};
```

- [ ] **Step 3: Wire it up**

Em `login.js:83-86`, adicionar a chamada de `ligarValidacaoAoVivo` para `cpf`:

```javascript
ligarValidacaoAoVivo(cadastroForm.nome, validadores.nome);
ligarValidacaoAoVivo(cadastroForm.email, validadores.email);
ligarValidacaoAoVivo(cadastroForm.cpf, validadores.cpf);
ligarValidacaoAoVivo(cadastroForm.senha, validadores.senha);
if (loginEmailInput) ligarValidacaoAoVivo(loginEmailInput, validadores.email);
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/js/login.js
git commit -m "feat(frontend): envia e valida cpf no cadastro"
```

---

### Task 13: Verificação manual no navegador

Sem isso a feature não está pronta — a suíte automatizada cobre o backend, mas o formulário só
foi editado, nunca clicado.

- [ ] **Step 1: Start the app**

Run: `.\mvnw.cmd spring-boot:run`

(Se o app já estava rodando de uma sessão anterior, mate o processo antigo primeiro — edições em
`templates/`/`static/` não hot-reload em `spring-boot:run` já em execução, conforme já registrado
na memória do projeto.)

- [ ] **Step 2: Open the signup form and confirm the field renders**

Abrir `http://localhost:8080/login`, clicar em "Cadastre-se", confirmar visualmente que o campo
CPF aparece entre Email e Senha, com o mesmo estilo dos outros campos (ícone, borda, foco).

- [ ] **Step 3: Test the live validation**

Digitar um CPF com menos de 11 dígitos → grupo fica com borda vermelha (`.invalido`). Completar
para 11 dígitos → borda fica verde (`.valido`). Confirma que Step 2/3 da Task 12 estão
funcionando.

- [ ] **Step 4: Submit with an invalid CPF and confirm the backend rejects it**

Preencher nome/email/senha válidos e um CPF com 11 dígitos mas dígito verificador errado (ex:
`11144477736`). Submeter. Confirmar que a mensagem de erro do backend aparece
(`cadastro-mensagem`), não um "sucesso" falso.

- [ ] **Step 5: Submit with a valid CPF and confirm the account is created**

Preencher um CPF real e válido (o próprio, ou qualquer CPF válido gerado por algoritmo).
Submeter. Confirmar: toggle de volta pra tela de login, mensagem de sucesso, email pré-preenchido
— mesmo fluxo já existente, agora passando pelo CPF também.

- [ ] **Step 6: Confirm duplicate CPF is rejected**

Tentar cadastrar uma segunda conta com o mesmo CPF do Step 5 (email diferente). Confirmar que a
API retorna erro e a mensagem aparece no formulário.

- [ ] **Step 7: Check the browser console/network tab**

Sem erros novos no console, `POST /usuarios` no Network tab mostra o `cpf` como string de 11
dígitos no corpo da requisição (sem pontuação), mesmo com pontuação digitada no campo.
