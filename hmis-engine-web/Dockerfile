# syntax=docker/dockerfile:1.7

# ---- build stage ----
# Installs npm deps cleanly (npm ci) and runs the Angular production build.
# Angular CLI emits to dist/hmis-engine-web/browser as of v17+.
FROM node:20-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---- runtime stage ----
# nginx serves the static bundle and proxies /api/* to the api service.
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Replace default site with our SPA + proxy config.
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /workspace/dist/hmis-engine-web/browser/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=5 \
    CMD wget -q -O- http://localhost/healthz || exit 1
