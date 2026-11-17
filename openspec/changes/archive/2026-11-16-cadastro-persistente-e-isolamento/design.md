## Modelagem de ameacas (STRIDE)

| Ameaca | No Rendo | Controle |
| --- | --- | --- |
| **S**poofing | Acessar a conta de outra pessoa copiando link/URL | Dados vem da sessao (`/me`); rotas com id exigem dono ou ADMIN; testes de isolamento |
| **T**ampering | Usuario se promover a ADMIN | Papel so via `ADMIN_EMAILS` no ambiente do servidor; nenhum endpoint altera papel |
| **I**nformation disclosure | Vazamento do banco expoe CPF; API devolvia CPF; 409 ecoava CPF | CPF so como HMAC com chave fora do banco; campo removido da API e da mensagem |
| **E**levation of privilege | Rotas administrativas por id | Regras de papel existentes + testes cobrindo cada rota com id |

## Decisions

### D1. HMAC-SHA256 com chave secreta, nao hash simples nem criptografia
CPF tem so 10^9 combinacoes validas: SHA-256 puro seria revertido por forca bruta em minutos. Com
HMAC e chave fora do banco, um vazamento do banco nao revela CPFs. Criptografia reversivel foi
rejeitada: nenhuma funcionalidade precisa ler o CPF de volta, e guardar algo reversivel e guardar
o dado sensivel. A chave vem de `CPF_HASH_KEY`; o profile dev tem chave fixa de desenvolvimento;
fora do dev, sem a variavel, a aplicacao nao sobe (fail-closed). Trocar a chave invalida a checagem
de duplicidade dos CPFs ja gravados -- documentado no `.env.example`.

### D2. Migracao na inicializacao
`ddl-auto=update` nao migra dados. Um runner na inicializacao converte linhas cujo `cpf` ainda tem
11 digitos (texto puro) para o hash; idempotente, porque o hash tem 64 caracteres hex.

### D3. H2 em arquivo no dev; banco proprio nos testes
`jdbc:h2:file:./data/rendo-dev` com `ddl-auto=update` preserva os cadastros entre reinicios. Os
testes sobrescrevem a datasource do profile dev com H2 em memoria
(`src/test/resources/config/application-dev.properties`, que tem precedencia sobre o arquivo de
`main`), para nao gravar nem apagar dados reais do desenvolvedor.

### D4. ADMIN por configuracao
`ADMIN_EMAILS` (lista separada por virgula) e lida no cadastro e por um runner na inicializacao. O
email do administrador fica no `.env`, fora do repositorio. Um terceiro que cadastre esse email
primeiro nao consegue entrar: o login exige a verificacao do codigo enviado ao proprio email.

## Risks / Trade-offs

- Perda da `CPF_HASH_KEY` = duplicidade de CPF deixa de ser detectada para registros antigos.
- O arquivo H2 do dev nao e criptografado; contem nome, email, hash de senha e hash de CPF -- nenhum
  dado sensivel em claro.
