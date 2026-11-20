## Why

O portão de segurança cobre SAST, segredos, bytecode e dependências, mas nenhuma camada testa a aplicação rodando. O ZAP do Módulo 11 tinha sido executado à mão uma vez. Um DAST reproduzível pega o que só aparece na resposta HTTP real: cabeçalhos, formulários e CSP.

## What Changes

- **`scripts/dast_zap.sh`** (novo): sobe uma instância descartável (H2 em memória, email desligado, sem ADMIN) e roda o ZAP baseline passivo, com imagem de versão fixada. É fail-closed: qualquer alerta não triado reprova.
- **`security/zap-baseline.conf`** (novo): uma linha com justificativa por alerta aceito.
- **Correções dos achados:**
  - CSP restrita: `default-src 'self'`, `script-src 'self'`, `form-action 'self'`, origens externas só Google Fonts e logos (`img-src https:`). Fica fora do `/h2-console`, que só existe em dev.
  - O script de tema inline virou `js/tema-inicial.js`, ainda síncrono e antes dos stylesheets. O `onerror` inline dos logos virou um listener.
  - `Cross-Origin-Opener-Policy: same-origin`.
  - Os formulários de cadastro e verificação viraram `POST` com token CSRF. Sem JS, senha e CPF iam na URL via GET.

## Impact

- **Endpoints públicos e banco:** sem mudança. Só cabeçalhos de resposta e markup.
- **Dependências:** nenhuma dependência Maven nova. O ZAP roda por imagem Docker, como o gitleaks e o OSV.
