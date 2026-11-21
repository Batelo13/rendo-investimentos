## Why

O gate de segurança e o DAST só rodavam quando alguém lembrava. CVEs novas em dependências e regras novas do semgrep aparecem sem nenhum commit no projeto, então o código parado também precisa ser reavaliado.

## What Changes

- **`.github/workflows/seguranca.yml`** (novo): roda toda segunda às 06:00 (Brasília) e também sob demanda (`workflow_dispatch`).
  - **Job `gate`:** executa `scripts/security_gate.sh`.
  - **Job `dast`:** executa `scripts/dast_zap.sh`.
  - Os relatórios ficam como artefato por 30 dias.
- **`mvnw` e `scripts/dast_zap.sh`** passam a ser executáveis no Git, o que o Linux exige.
- **Achado `UNVALIDATED_REDIRECT`** que o gate pegou no limite de login: o redirect passa pelo `DefaultRedirectStrategy`.

## Impact

- **Endpoints e banco:** sem mudança.
- **Dependências Maven:** nenhuma. O CI usa as mesmas ferramentas fixadas dos scripts.
