#!/usr/bin/env bash
# DAST do Rendo (Modulo 11): OWASP ZAP baseline (passivo, sem ataques) contra
# uma instancia descartavel da aplicacao -- banco H2 em memoria, email desligado,
# nenhum ADMIN. Nunca aponta para os dados reais do desenvolvedor.
# Sai com 1 se o ZAP achar alerta nao triado OU nao puder rodar (fail-closed).
# Regras aceitas/ignoradas: security/zap-baseline.conf (cada uma com justificativa).
# Relatorios em security/reports/zap.{html,json} (ignorado pelo Git).
#
# Requisitos: Java 17 e Docker em execucao.
set -uo pipefail
cd "$(dirname "$0")/.."
docker() { MSYS_NO_PATHCONV=1 command docker "$@"; }
PORTA=${DAST_PORTA:-8081}
mkdir -p security/reports

if ! docker info >/dev/null 2>&1; then
  echo "REPROVADO: Docker indisponivel -- ZAP nao pode rodar (fail-closed)."
  exit 1
fi

echo "== 1/3 Empacotando a aplicacao"
./mvnw -q -B package -DskipTests || { echo "REPROVADO: build falhou"; exit 1; }

echo "== 2/3 Subindo instancia descartavel na porta $PORTA"
java -jar target/gestao-investimentos-0.0.1-SNAPSHOT.jar \
  --server.port="$PORTA" \
  "--spring.datasource.url=jdbc:h2:mem:dast;DB_CLOSE_DELAY=-1" \
  --spring.jpa.hibernate.ddl-auto=create-drop --spring.jpa.show-sql=false \
  --app.mail.enabled=false --app.mail.log-codigo=false --app.admin.emails= \
  > security/reports/dast-app.log 2>&1 &
APP=$!
trap 'kill $APP 2>/dev/null' EXIT

for _ in $(seq 1 90); do
  curl -fs -o /dev/null "http://localhost:$PORTA/" && break
  kill -0 $APP 2>/dev/null || { echo "REPROVADO: app nao subiu (security/reports/dast-app.log)"; exit 1; }
  sleep 1
done
curl -fs -o /dev/null "http://localhost:$PORTA/" || { echo "REPROVADO: app nao respondeu em 90s"; exit 1; }

echo "== 3/3 ZAP baseline"
# host-gateway: faz host.docker.internal funcionar tambem no Docker do Linux/CI.
docker run --rm --add-host=host.docker.internal:host-gateway \
  -v "$PWD/security:/zap/wrk:rw" ghcr.io/zaproxy/zaproxy:2.16.1 \
  zap-baseline.py -t "http://host.docker.internal:$PORTA" -m 2 \
  -c zap-baseline.conf -r reports/zap.html -J reports/zap.json
CODIGO=$?

# 0 = sem alertas; 1 = FAIL; 2 = WARN; 3 = erro do proprio ZAP. Tudo != 0 reprova.
if [ $CODIGO -ne 0 ]; then
  echo "REPROVADO: ZAP saiu com $CODIGO (detalhes em security/reports/zap.html)"
  exit 1
fi
echo "APROVADO: ZAP baseline sem alertas nao triados"
