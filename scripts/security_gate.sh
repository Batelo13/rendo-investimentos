#!/usr/bin/env bash
# Portao de seguranca do Rendo (Modulo 11, secao 16 do curso desenvolvimento-assistido-IA).
# Roda igual em qualquer maquina, em qualquer CI, com ou sem agente de IA.
# Sai com 1 se alguma camada reprovar OU nao puder rodar (fail-closed).
# Relatorios em security/reports/ (ignorado pelo Git).
#
# Requisitos: Java 17, uv (para o semgrep via uvx) e Docker em execucao
# (gitleaks e OSV-Scanner usam imagens com versao fixada).
set -uo pipefail
cd "$(dirname "$0")/.."
# Git Bash (Windows): nao converter "/repo" em "C:/Program Files/Git/repo" so nos
# argumentos do docker, e gravar o SARIF do semgrep em UTF-8 (padrao cp1252
# quebra a escrita). Inofensivos em Linux/macOS/CI.
export PYTHONUTF8=1
docker() { MSYS_NO_PATHCONV=1 command docker "$@"; }
mkdir -p security/reports
FALHAS=()

if ! docker info >/dev/null 2>&1; then
  echo "REPROVADO: Docker indisponivel -- gitleaks e OSV-Scanner nao podem rodar (fail-closed)."
  exit 1
fi

echo "== 1/5 SAST: semgrep (registro, sem telemetria)"
uvx semgrep@1.178.0 scan --metrics=off --error --quiet \
  --config p/java --config p/secrets --config p/owasp-top-ten \
  --sarif --output security/reports/semgrep.sarif src/ || FALHAS+=("semgrep")

echo "== 2/5 Segredos: gitleaks (historico Git inteiro + arquivos atuais)"
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:v8.30.1 git /repo --no-banner --redact \
  --report-format sarif --report-path /repo/security/reports/gitleaks-historico.sarif || FALHAS+=("gitleaks-historico")
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:v8.30.1 dir /repo/src --no-banner --redact \
  --report-format sarif --report-path /repo/security/reports/gitleaks-src.sarif || FALHAS+=("gitleaks-src")

echo "== 3/5 Bytecode: SpotBugs + FindSecBugs"
./mvnw -q -B compile spotbugs:check || FALHAS+=("spotbugs")

echo "== 4/5 Dependencias: SBOM CycloneDX + OSV-Scanner"
./mvnw -q -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom \
    -DoutputFormat=json -DincludeTestScope=false \
  && docker run --rm -v "$PWD:/src" ghcr.io/google/osv-scanner:v2.6.0 scan source -L /src/target/bom.json \
       --format sarif --output-file /src/security/reports/osv.sarif || FALHAS+=("osv-scanner")

echo "== 5/5 Specs: OpenSpec estrito"
openspec validate --all --strict || FALHAS+=("openspec")

if [ ${#FALHAS[@]} -gt 0 ]; then
  echo "REPROVADO: ${FALHAS[*]} (detalhes em security/reports/)"
  exit 1
fi
echo "APROVADO: nenhuma camada encontrou problema"
