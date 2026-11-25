## Decisões

1. **Gatilhos:** push na `27-develop` (onde o trabalho acontece), pull request para a `main` (antes do merge) e `workflow_dispatch`. Push na `main` fica de fora, porque ninguém faz push direto nela (módulo 11).
2. **`clean verify`**, não `test`: só o `verify` chega ao relatório e à checagem do JaCoCo (aula 15).
3. **Sem segredos no CI.** Os testes rodam no perfil dev, que tem valores locais para a chave do JWT e o HMAC do CPF, com o banco H2 em memória dos testes e o envio de e-mail desligado. Nenhum `secrets.*` é usado.
4. **Permissões mínimas** (`contents: read`), actions fixadas por SHA e `timeout-minutes` para não deixar job travado consumindo minutos.
5. **Concorrência:** um push novo na mesma branch cancela a execução anterior.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não se aplica: sem credenciais. |
| Tampering | Actions fixadas por SHA (tag é mutável). O token tem só `contents: read`. |
| Repudiation | O histórico de execuções do GitHub registra cada rodada. |
| Information disclosure | Os artefatos são relatórios de teste e de cobertura, sem segredos. |
| Denial of service | `timeout-minutes: 30` e cancelamento de execuções antigas. |
| Elevation of privilege | Pull request de fork roda sem segredos e com token só de leitura (padrão do GitHub). |
