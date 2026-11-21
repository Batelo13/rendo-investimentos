## 1. CI

- [x] 1.1 Workflow semanal + manual com actions fixadas por SHA e token só leitura. Teste: `actionlint` 1.7.7 sem erros.
- [x] 1.2 `mvnw` e `dast_zap.sh` executáveis. Teste: `git ls-files -s` com modo 100755.
- [x] 1.3 Achado `UNVALIDATED_REDIRECT` do SpotBugs corrigido. Teste: `bash scripts/security_gate.sh` APROVADO e `LimiteTentativasLogin*` verdes.
- [x] 1.4 Primeira execução no GitHub (`workflow_dispatch`) depois do push. Teste: run 44854126134 verde (gate e DAST).
