#!/usr/bin/env bash
# Container entrypoint: bring up Postgres, the API, then nginx (foreground).
set -euo pipefail

PG_VER="$(ls /etc/postgresql)"

# 1. Start the install-time Postgres cluster (data is ephemeral).
pg_ctlcluster "$PG_VER" main start
until su postgres -c "psql -c '\q'" 2>/dev/null; do sleep 1; done

# 2. App role + database (idempotent).
su postgres -c "psql -tAc \"SELECT 1 FROM pg_roles WHERE rolname='hmis'\" | grep -q 1" \
  || su postgres -c "psql -c \"CREATE ROLE hmis LOGIN PASSWORD 'hmis';\""
su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname='hmis_engine'\" | grep -q 1" \
  || su postgres -c "psql -c \"CREATE DATABASE hmis_engine OWNER hmis;\""

# 3. Backend — Flyway migrates V1..V48 on boot; ROOT user is bootstrapped.
java -jar /app/app.jar \
  --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/hmis_engine \
  --spring.datasource.username=hmis \
  --spring.datasource.password=hmis \
  --logging.level.org.hibernate.SQL=WARN \
  --logging.level.com.otapp.hmis.engine=INFO \
  > /var/log/hmis-api.log 2>&1 &

# 4. Wait for health, then serve the SPA + proxy in the foreground.
until curl -sf http://127.0.0.1:8080/api/actuator/health >/dev/null; do sleep 2; done
echo "Backend healthy — starting nginx."
exec nginx -g 'daemon off;'
