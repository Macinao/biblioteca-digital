#!/usr/bin/env bash
# Aplica a topologia inteira (Fase 8) num cluster kind local, reaproveitando
# as MESMAS imagens Docker e os MESMOS arquivos de config do docker-compose
# (observability/) — nada duplicado, so traduzido pro formato do K8s.
set -euo pipefail

cd "$(dirname "$0")/.."

CLUSTER=smartteca

echo "==> Carregando imagens locais no cluster kind (sem registry)..."
for svc in core-service search-service storage-service notification-service chatbot-service api-gateway; do
  kind load docker-image "portifolio00-${svc}:latest" --name "$CLUSTER"
done

echo "==> Gerando k8s/config/secret.yaml a partir do .env (arquivo gitignored, nunca comitado)..."
set -o allexport
source .env
set +o allexport

cat > k8s/config/secret.yaml <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: smartteca-secrets
  namespace: smartteca
type: Opaque
stringData:
  DB_PASSWORD: "${DB_PASSWORD:-smartteca}"
  JWT_PRIVATE_KEY: "${JWT_PRIVATE_KEY}"
  JWT_PUBLIC_KEY: "${JWT_PUBLIC_KEY}"
  RABBITMQ_PASSWORD: "${RABBITMQ_PASSWORD:-guest}"
  EVOLUTION_DB_PASSWORD: "${EVOLUTION_DB_PASSWORD:-123456}"
  EVOLUTION_APIKEY: "${EVOLUTION_APIKEY:-123456}"
  SUPABASE_KEY: "${SUPABASE_KEY:-}"
  AI_API_KEY: "${AI_API_KEY:-ollama}"
  GRAFANA_ADMIN_PASSWORD: "${GRAFANA_ADMIN_PASSWORD:-admin}"
EOF

echo "==> Namespace, config e secret..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/config/configmap.yaml
kubectl apply -f k8s/config/secret.yaml

echo "==> Infra (Postgres, Kafka, RabbitMQ, Elasticsearch, Redis, Evolution)..."
kubectl apply -f k8s/infra/

echo "==> ConfigMaps do Grafana, gerados dos mesmos arquivos do docker-compose..."
kubectl create configmap grafana-datasources \
  --from-file=observability/grafana/provisioning/datasources \
  -n smartteca --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboards-provider \
  --from-file=observability/grafana/provisioning/dashboards \
  -n smartteca --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboards \
  --from-file=observability/grafana/dashboards \
  -n smartteca --dry-run=client -o yaml | kubectl apply -f -

echo "==> Observabilidade (Prometheus, Grafana, kafka-exporter)..."
kubectl apply -f k8s/observability/

echo "==> Servicos SmartTeca..."
kubectl apply -f k8s/app/

echo
echo "==> Pronto. Acompanhe com: kubectl get pods -n smartteca -w"
echo "==> Gateway (apos os pods ficarem Ready): http://localhost:8888"
