# QA Deployment — single-container on an AWS EC2 Linux instance

A throwaway, **all-in-one** deployment for QA testing. Everything (PostgreSQL +
Spring Boot API + the built Angular SPA behind nginx) runs in **one Docker
container**. Data is **ephemeral** — every container start re-runs Flyway from an
empty database and re-bootstraps the ROOT user. **Not for production.**

```
                    EC2 instance (Amazon Linux 2023, Docker host)
   browser ──▶  :80 ┌──────────────────── one container ─────────────────────┐
                    │  nginx  ──/──▶ Angular SPA (static)                      │
                    │         ──/api─▶ 127.0.0.1:8080  Spring Boot (hmis-api)  │
                    │                         └──▶ 127.0.0.1:5432  PostgreSQL  │
                    └─────────────────────────────────────────────────────────┘
```

Why this works with no extra config:
- The frontend production build already calls `apiUrl: '/api'` (relative), so nginx
  serves the SPA and reverse-proxies `/api` on the **same origin** → no CORS.
- The API's context path is `/api`; nginx proxies `/api/ → 127.0.0.1:8080/api/`.
- Postgres is local to the container; the API points at it via command-line overrides.

---

## 0. Prerequisites (on your laptop)

- **AWS CLI v2** configured (`aws configure`) with rights to create EC2 instances,
  security groups and key pairs.
- An SSH client and `rsync` (Git Bash / WSL / macOS / Linux).
- Pick a region and keep it consistent. The commands below use shell variables.

```bash
export AWS_REGION=us-east-1          # adjust
export KEY=hmis-qa-key
export SG=hmis-qa-sg
```

---

## 1. Provision the instance (laptop)

```bash
# 1a. Key pair (saved locally)
aws ec2 create-key-pair --region "$AWS_REGION" --key-name "$KEY" \
  --query KeyMaterial --output text > "$KEY.pem"
chmod 400 "$KEY.pem"

# 1b. Security group — SSH (22) + HTTP (80) limited to YOUR current IP
MYIP="$(curl -s https://checkip.amazonaws.com)/32"
SG_ID=$(aws ec2 create-security-group --region "$AWS_REGION" \
  --group-name "$SG" --description "HMIS QA" --query GroupId --output text)
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges="[{CidrIp=$MYIP}]"
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --ip-permissions IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=$MYIP}]"

# 1c. Launch — t3.medium (2 vCPU / 4 GB) + 30 GB gp3. AMI resolved via SSM.
IID=$(aws ec2 run-instances --region "$AWS_REGION" \
  --image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --instance-type t3.medium \
  --key-name "$KEY" --security-group-ids "$SG_ID" \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3}' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=hmis-qa}]' \
  --query 'Instances[0].InstanceId' --output text)

aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$IID"
export PUBDNS=$(aws ec2 describe-instances --region "$AWS_REGION" --instance-ids "$IID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)
echo "Instance $IID  →  http://$PUBDNS"
```

> Notes
> - `t3.medium` is enough because the Maven and Angular builds run **sequentially**
>   inside the Docker multi-stage build. Step 3 adds 2 GB swap as a safety margin.
> - HTTP is locked to your IP. Widen `--ip-permissions ... CidrIp=0.0.0.0/0` only if
>   QA testers are on other networks (it's a throwaway box, but still your call).

---

## 2. Ship the code to the instance (laptop)

From the repo root (`d:\My_Works\HMS\HMSCLEAN`), sync the source — excluding build
junk so the upload is small:

```bash
rsync -az --delete \
  -e "ssh -i $KEY.pem -o StrictHostKeyChecking=accept-new" \
  --exclude '.git' --exclude 'node_modules' --exclude 'target' \
  --exclude 'dist' --exclude '*.pem' \
  ./ ec2-user@"$PUBDNS":~/hmis/
```

*(Alternative: `git clone https://<token>@github.com/godfreydesidery/HMSCLEAN.git hmis`
on the instance if you'd rather pull than push.)*

---

## 3. One-shot build & run (on the instance)

SSH in and run a single deploy script. It installs Docker, writes the three
container files, builds the single image, and runs it.

```bash
ssh -i "$KEY.pem" ec2-user@"$PUBDNS"
```

Then, on the instance:

```bash
cd ~/hmis

# --- host setup: Docker + swap (idempotent) ---
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
if ! sudo swapon --show | grep -q /swapfile; then
  sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
fi

# --- container build files ---
mkdir -p deploy

cat > .dockerignore <<'EOF'
.git
**/node_modules
**/target
**/dist
*.pem
*.md
EOF

cat > deploy/Dockerfile <<'EOF'
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

# ---------- stage 3: runtime (postgres + jre + nginx) ----------
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
EOF

cat > deploy/nginx.conf <<'EOF'
server {
  listen 80 default_server;
  root /usr/share/nginx/html;
  index index.html;

  # API → Spring Boot (context path /api)
  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  # SPA fallback (Angular client-side routing)
  location / {
    try_files $uri $uri/ /index.html;
  }
}
EOF

cat > deploy/start.sh <<'EOF'
#!/usr/bin/env bash
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
EOF

# --- build the single image (first run ~5–10 min: downloads Maven + npm deps) ---
sudo docker build -t hmis-qa -f deploy/Dockerfile .

# --- run it ---
sudo docker rm -f hmis-qa 2>/dev/null || true
sudo docker run -d --name hmis-qa --restart unless-stopped -p 80:80 \
  -e HMIS_SECURITY_JWT_SECRET='qa-please-change-this-32char-minimum!!' \
  -e HMIS_BOOTSTRAP_ROOT_PASSWORD='QaRoot!123' \
  hmis-qa

# Watch it come up (Flyway + Spring start ~15–25s)
sudo docker logs -f hmis-qa
```

---

## 4. Verify

- Open **`http://<PUBDNS>`** (printed in step 1c) in a browser.
- Log in as **`root`** / the `HMIS_BOOTSTRAP_ROOT_PASSWORD` you set (`QaRoot!123`
  above) — you'll be forced to set a new password on first login.
- Quick API check from anywhere:
  ```bash
  curl -i http://<PUBDNS>/api/actuator/health      # {"status":"UP"}
  ```
- Container internals if needed:
  ```bash
  sudo docker exec -it hmis-qa tail -f /var/log/hmis-api.log
  ```

The DB starts empty (only the ROOT user + IAM seed). Build up clinics, masterdata,
patients, etc. as part of QA.

---

## 5. Redeploy after code changes

```bash
# laptop: re-sync
rsync -az --delete -e "ssh -i $KEY.pem" \
  --exclude '.git' --exclude 'node_modules' --exclude 'target' --exclude 'dist' --exclude '*.pem' \
  ./ ec2-user@"$PUBDNS":~/hmis/

# instance: rebuild + restart (wipes data — that's intended for QA)
cd ~/hmis
sudo docker build -t hmis-qa -f deploy/Dockerfile .
sudo docker rm -f hmis-qa
sudo docker run -d --name hmis-qa --restart unless-stopped -p 80:80 \
  -e HMIS_SECURITY_JWT_SECRET='qa-please-change-this-32char-minimum!!' \
  -e HMIS_BOOTSTRAP_ROOT_PASSWORD='QaRoot!123' \
  hmis-qa
```

> Want data to survive a container *restart* (but still be throwaway overall)? Run
> Postgres on a named volume: add `-v hmis-pgdata:/var/lib/postgresql` to `docker run`.
> Omit it (as above) for fully ephemeral.

---

## 6. Teardown (stop paying)

```bash
aws ec2 terminate-instances --region "$AWS_REGION" --instance-ids "$IID"
aws ec2 wait instance-terminated --region "$AWS_REGION" --instance-ids "$IID"
aws ec2 delete-security-group --region "$AWS_REGION" --group-id "$SG_ID"
aws ec2 delete-key-pair --region "$AWS_REGION" --key-name "$KEY"
rm -f "$KEY.pem"
```

---

## Caveats (QA only)

- **Plaintext-ish secrets**: the JWT secret and ROOT password are passed as env on
  the command line; Postgres uses `hmis/hmis`. Fine for a locked-down throwaway box,
  not for anything real.
- **HTTP only** (no TLS). Add a reverse proxy / ACM + ALB, or Caddy, if QA needs HTTPS.
- **One container, no isolation** between DB and app — deliberate for simplicity and
  ephemerality. Production should split Postgres (RDS) from the app and not bake the
  DB into the image.
- **Single instance, no backups, data wiped on rebuild** — by design.
- If the Maven/npm build ever OOMs on `t3.medium`, bump to `t3.large` (8 GB) for the
  build, or keep the 2 GB swap from step 3.
