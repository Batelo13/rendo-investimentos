## Decisões

- **O CI chama os scripts, não reimplementa.** É o mesmo comando local e no CI, como o Módulo 11 pede: o gate roda igual em qualquer máquina.
- **Actions fixadas por SHA**, com a versão em comentário. Tag de action é mutável, e o gate de segurança não pode ser ele mesmo a porta de entrada da cadeia de suprimentos.
- **`permissions: contents: read`.** O workflow não precisa escrever nada no repositório.
- **Agendado + manual, sem rodar em PR por enquanto.** O gate leva vários minutos, e o fluxo atual já roda o gate localmente antes de cada PR.
- **`chmod a+w security` antes do DAST.** O ZAP roda como uid 1000 no container, e o runner usa outro uid para o checkout.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não se aplica. |
| Tampering | As actions são fixadas por SHA, e o token do workflow é só leitura. |
| Repudiation | O histórico de execuções e os artefatos ficam no GitHub. |
| Information disclosure | Repositório público: os relatórios são artefatos visíveis. O gitleaks roda com `--redact`. O DAST usa instância descartável, sem segredos (o `.env` não existe no CI). |
| Denial of service | `timeout-minutes: 30` por job. |
| Elevation of privilege | Sem `pull_request_target` nem segredos no workflow. |
