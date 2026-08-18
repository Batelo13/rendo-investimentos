# Cadastro de CPF do Usuário — Design

Data: 2026-08-18
Branch prevista: `14-melhorias-frontend` (ou nova branch a partir dela)

## Contexto

`Usuario` hoje é cadastrado só com `nome`/`email`/`senha` (`UsuarioRequestDTO`,
`UsuarioService.cadastrar`). O padrão de validação "real" já existe pra `Corretora`: CNPJ é
validado consultando a BrasilAPI (dado público de empresa). Pessoa física não tem equivalente —
CPF é dado protegido, não existe API pública de "consulta CPF → nome". Então esta feature não
busca dado nenhum externo: valida o próprio número (formato + dígito verificador, algoritmo
padrão mod-11) e garante unicidade no banco, mesmo espírito de rigor que o CNPJ tem, sem
depender de terceiro.

## Escopo

- Campo `cpf` obrigatório e único em `Usuario`.
- Validação de formato (11 dígitos) + dígito verificador via uma anotação Bean Validation
  customizada (`@CPF`), aplicada no `UsuarioRequestDTO` do mesmo jeito que `@Email`/`@NotBlank`
  já são.
- `cpf` exposto em `UsuarioResponseDTO`, mesmo tratamento que `email` já tem hoje.
- Campo novo no formulário de cadastro (`login.html`/`login.js`), com validação client-side
  leve (só comprimento) — o dígito verificador de verdade é responsabilidade do backend, mesma
  filosofia que os outros campos desse form já seguem ("a validação de verdade continua sendo a
  do backend", comentário já existente em `login.js`).

Fora de escopo (adiar/não construir agora):

- Qualquer lookup externo de CPF — não existe API pública equivalente à BrasilAPI para pessoa
  física; forçar isso seria simular precisão que não existe.
- Máscara de input (`000.000.000-00`) formatando enquanto digita — mesmo padrão do campo CNPJ
  hoje (`dashboard.js`), que aceita texto livre e só extrai dígitos no submit
  (`onlyDigits`/`fmtCnpj`). CPF segue a mesma convenção pra não introduzir um padrão de UI novo
  só pra este campo.
- Tornar o campo opcional/nullable — decisão explícita: obrigatório desde já, sem migração de
  dados legados a considerar (schema é `create-drop` em dev, sem usuários reais).

## Modelo de dados

### `Usuario`

Novo campo:

- `cpf` (`String`) — `@Column(nullable = false, unique = true)`. Guardado como 11 dígitos puros,
  sem pontuação — mesma convenção de `Corretora.cnpj` (14 dígitos puros).

Sem migração necessária: `spring.jpa.hibernate.ddl-auto=create-drop` em dev recria o schema.

## Validação: `@CPF` (novo)

Novo pacote `validation/` (primeiro validador puro do projeto que não depende de dado externo —
`CvmValidador` fica em `integration/` porque opera sobre `DadosCnpjResponse` já buscado na API;
este aqui só olha a própria string).

- `CPF` — anotação `@Target({FIELD, PARAMETER})`, `@Retention(RUNTIME)`,
  `@Constraint(validatedBy = CpfValidator.class)`, mensagem padrão "CPF inválido".
- `CpfValidator implements ConstraintValidator<CPF, String>` — algoritmo padrão de dígito
  verificador do CPF (mod-11, dois dígitos calculados a partir dos 9 primeiros): recalcula os
  dois dígitos verificadores e compara com os informados. Rejeita também sequências com todos os
  dígitos iguais (`00000000000`, `11111111111`, ...), caso clássico que passa no mod-11 por
  degenerescência do algoritmo mas nunca é um CPF real emitido.
- `isValid` retorna `true` para `null`/vazio — igual ao padrão do Bean Validation (a
  obrigatoriedade é responsabilidade do `@NotBlank`, que já está sendo usado junto, não do
  validador de formato).

### `UsuarioRequestDTO`

```java
@NotBlank(message = "CPF e obrigatorio")
@Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 digitos numericos, sem pontuacao")
@CPF(message = "CPF invalido")
String cpf
```

Mesma composição de anotações que `email` já usa (`@NotBlank` + `@Email`).

### `UsuarioResponseDTO`

Novo campo `cpf`, mesma posição/tratamento que `email`.

## Mudanças em componentes existentes

### `UsuarioService.cadastrar()`

Novo check de duplicidade, mesmo padrão do check de email já existente:

```java
repository.findByCpf(dto.cpf()).ifPresent(existente -> {
    throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o CPF " + dto.cpf());
});
```

`usuario.setCpf(dto.cpf())` junto com os outros setters. `toResponseDTO` passa `usuario.getCpf()`.

### `UsuarioRepository`

Novo método `Optional<Usuario> findByCpf(String cpf)`, mesmo padrão de `findByEmail` já
existente.

### `login.html`

Novo `<div class="input-group">` no form de cadastro, entre `email` e `senha` (ou após `nome`
— ordem exata é detalhe de implementação, não muda a lógica): ícone SVG (mesmo estilo dos
outros campos), `<input type="text" name="cpf" placeholder="CPF" required>`.

### `login.js`

- `onlyDigits(v)` — novo helper local (mesma função que `dashboard.js` já tem, mas `login.js`
  não importa `dashboard.js` nem tem um módulo compartilhado hoje — duplicar a função de 1 linha
  é mais simples que criar um arquivo `utils.js` só pra isso agora).
- `dto.cpf = onlyDigits(cadastroForm.cpf.value)` adicionado ao objeto enviado em `POST
  /usuarios`.
- `validadores.cpf = (v) => onlyDigits(v).length === 11` — validação ao vivo de comprimento
  apenas (mesmo nível de rigor client-side que `nome`/`senha` já têm: feedback visual, não
  substitui o backend). `ligarValidacaoAoVivo(cadastroForm.cpf, validadores.cpf)` adicionado
  junto dos outros.

## Erros e validações

- CPF ausente/vazio: 400, `@NotBlank`.
- CPF com formato errado (não são 11 dígitos): 400, `@Pattern`.
- CPF com formato certo mas dígito verificador inválido: 400, `@CPF`.
- CPF duplicado (já existe usuário com esse CPF): 409, `RecursoDuplicadoException` — mesmo
  status que email duplicado já retorna hoje.

## Testes

- **`CpfValidatorTest`** (novo, unit puro, sem Spring): CPFs válidos conhecidos (gerados por
  algoritmo, não são CPFs reais de ninguém) retornam `true`; dígito verificador alterado retorna
  `false`; todos os dígitos iguais retorna `false`; string vazia/`null` retorna `true` (deixa
  `@NotBlank` cuidar disso).
- **`UsuarioService`/`UsuarioController` (unit + integration existentes)**: helpers que hoje
  criam `UsuarioRequestDTO`/`Usuario` de teste passam a incluir um CPF válido fixo. Novos casos:
  CPF duplicado → 409; CPF com dígito verificador inválido → 400.

## Self-Review

**Placeholder scan:** nenhum "TBD"/"TODO" — as 4 perguntas do brainstorming (nível de validação,
onde vive o checksum, obrigatório/opcional, exposição na resposta) estão todas resolvidas aqui.

**Consistência interna:** `cpf` segue exatamente a convenção já usada por `cnpj`
(`Corretora`/`CorretoraRequestDTO`) — dígitos puros, `@Pattern` de tamanho fixo, unicidade no
banco, sem máscara de input.

**Fora de escopo, e por quê:** lookup externo (não existe API pública equivalente); máscara de
input (mantém o mesmo padrão do campo CNPJ existente); campo opcional (decisão explícita do
usuário, sem dado legado a migrar).
