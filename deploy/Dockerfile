# Single QA image: Postgres + Spring Boot API + Angular SPA behind nginx.
# Multi-stage so the EC2 host only needs Docker. See DEPLOY_QA.md.

# ---------- stage 1: backend jar ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /src
COPY hmis-engine-api ./hmis-engine-api
RUN cd hmis-engine-api && mvn -B -DskipTests clean package

# ---------- stage 2: frontend bundle ----------
FROM node:20 AS frontend
WORKDIR /web
COPY hmis-engine-web/package*.json ./
RUN npm ci
COPY hmis-engine-web ./
RUN npm run build          # production config (apiUrl=/api) -> dist/hmis-engine-web/browser

# ---------- stage 3: runtime ----------
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
      postgresql nginx curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY --from=backend  /src/hmis-engine-api/target/hmis-engine-api-*.jar /app/app.jar
COPY --from=frontend /web/dist/hmis-engine-web/browser                 /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/sites-available/default
COPY deploy/start.sh   /start.sh
RUN chmod +x /start.sh
EXPOSE 80
CMD ["/start.sh"]
