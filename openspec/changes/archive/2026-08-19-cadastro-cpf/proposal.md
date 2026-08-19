## Why

O cadastro de `Usuario` aceitava apenas nome/email/senha, sem nenhum identificador de pessoa física. CNPJ já é validado (formato + consulta real via BrasilAPI) para `Corretora`; CPF não tem equivalente público (é dado protegido, não existe API pública de "consulta CPF → nome"), então o mesmo nível de rigor precisava vir de outro lugar: validação local de formato + dígito verificador, mais unicidade no banco.

## What Changes

- Novo campo `cpf` obrigatório e único em `Usuario`, guardado como 11 dígitos puros (sem pontuação) — mesma convenção do `Corretora.cnpj`.
- Nova anotação Bean Validation `@CPF` + `CpfValidator`, validando formato (11 dígitos) e dígito verificador (algoritmo padrão mod-11), sem nenhuma chamada externa.
- `UsuarioService.cadastrar()` passa a checar duplicidade de CPF (mesmo padrão já usado para email) antes de persistir.
- `cpf` exposto em `UsuarioResponseDTO`, mesmo tratamento que `email` já recebe.
- Novo campo no formulário de cadastro (`login.html`/`login.js`), com validação client-side leve (só comprimento) — o dígito verificador de verdade é responsabilidade exclusiva do backend.

## Capabilities

### New Capabilities
- `cadastro-cpf`: cadastro de CPF do usuário com validação de formato + dígito verificador (sem lookup externo) e unicidade no banco.

### Modified Capabilities

## Impact

- Backend: `model/Usuario.java`, `repository/UsuarioRepository.java`, `dto/UsuarioRequestDTO.java`, `dto/UsuarioResponseDTO.java`, `service/UsuarioService.java`, novo pacote `validation/` (`CPF.java`, `CpfValidator.java`).
- Testes: `CpfValidatorTest` (novo), `UsuarioAuthIntegrationTest` (2 cenários novos: CPF inválido → 400, CPF duplicado → 409), `OperacaoIntegrationTest` (fixture de usuário ajustada), `util/CpfTestFixtures.java` (novo, gerador de CPF sintético válido para testes).
- Frontend: `templates/login.html`, `static/js/login.js`.
- Sem migração de banco necessária (`spring.jpa.hibernate.ddl-auto=create-drop` em dev).
- Já implementado e mergeado: branch `14-melhorias-frontend`, PR #18. Esta change documenta retroativamente o que já está em produção, a partir de `docs/superpowers/specs/2026-08-18-cadastro-cpf-design.md` e `docs/superpowers/plans/2026-08-18-cadastro-cpf.md`.
