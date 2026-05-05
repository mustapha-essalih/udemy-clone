#!/bin/bash
set -e

COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"

case "$1" in
  up)
    echo "Stopping any old containers..."
    docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    echo "Starting Elasticsearch + Kafka (KRaft mode)..."
    docker compose -f "$COMPOSE_FILE" up -d elasticsearch kafka
    echo "Waiting for Elasticsearch to be healthy..."
    until docker compose -f "$COMPOSE_FILE" exec elasticsearch curl -s -u elastic:9898 http://localhost:9200/_cluster/health 2>/dev/null | grep -qE '"status":"(green|yellow)"'; do
      echo "  ES not ready yet, waiting 5s..."
      sleep 5
    done
    echo "Elasticsearch is healthy."
    echo "Waiting for Kafka to be healthy..."
    until docker compose -f "$COMPOSE_FILE" exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; do
      echo "  Kafka not ready yet, waiting 5s..."
      sleep 5
    done
    echo "Kafka is healthy."
    echo ""
    echo "Infrastructure ready:"
    echo "  Elasticsearch: http://localhost:9200  (user: elastic / pass: 9898)"
    echo "  Kafka:         localhost:9092"
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down --remove-orphans
    ;;
  kibana)
    echo "Starting Kibana..."
    docker compose -f "$COMPOSE_FILE" --profile tools up -d kibana
    echo "Kibana: http://localhost:5601  (user: kibana_system / pass: 9898)"
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f "${2:-elasticsearch}"
    ;;
  status)
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  *)
    echo "Usage: $0 {up|down|kibana|logs [service]|status}"
    exit 1
    ;;
esac
