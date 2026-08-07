#!/usr/bin/env bash
# Tum servisleri paralel olarak local'de baslatir.
set -e
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for svc in search cart payment inventory; do
  echo "Starting ${svc}-service..."
  (cd "$ROOT_DIR/services/${svc}-service" && mvn spring-boot:run) &
done

wait
