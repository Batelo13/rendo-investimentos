## Decisões

- **Script separado do `security_gate.sh`.** O DAST precisa da aplicação rodando (build + boot, cerca de 1 min), e o gate atual roda sem ela. Ficam dois comandos independentes, ambos fail-closed.
- **Baseline passivo, sem autenticação.** O baseline não ataca: só observa o que o spider encontra. Ele cobre as páginas públicas e as respostas 401 da API. Um scan ativo e autenticado fica para quando houver ambiente de homologação.
- **Instância descartável.** H2 em memória com `create-drop`, email desligado e lista de ADMIN vazia. Nunca toca `data/rendo-dev` nem o SMTP real do `.env`.
- **Arquivo externo em vez de hash CSP para o script de tema.** O hash quebraria a cada mudança de espaço em branco. Um `<script src>` síncrono no `<head>` continua rodando antes do primeiro paint. O contrato de markup passa a exigir `tema-inicial.js` antes de `tokens.css` e nenhum `<script>` ou handler inline.
- **`style-src 'unsafe-inline'` aceito.** O JS do dashboard e o SweetAlert2 geram `style=`. Injeção de estilo é de baixo impacto quando o script está bloqueado.
- **COEP não aplicado.** `require-corp` bloquearia Google Fonts e logos externos sem cabeçalho CORP.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não há autenticação nova. O OAuth continua por link (`form-action` não se aplica a ele). |
| Tampering | A CSP `script-src 'self'` bloqueia script injetado (XSS refletido ou armazenado vira inerte). `form-action 'self'` impede desviar o envio de formulário para outra origem. |
| Repudiation | Não se aplica. |
| Information disclosure | Os formulários POST tiram senha e CPF de URL, histórico e logs de acesso. COOP isola a janela de outras origens. |
| Denial of service | O scan roda só contra a instância local descartável. |
| Elevation of privilege | Os formulários levam token CSRF também no envio nativo. Sem mudança em regras de acesso. |
