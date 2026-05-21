#!/usr/bin/env bash
# Runs ON the EC2 instance: ensure Docker + swap, build the single image, (re)run it.
# Invoked by deploy/aws-redeploy.sh over SSH. Reads ROOT_PW / JWT from the env.
set -euo pipefail
cd ~/hmis

# Host deps (idempotent). Package manager differs by AMI: dnf (Amazon Linux), apt (Ubuntu).
if ! command -v docker >/dev/null 2>&1; then
  if command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y docker
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io
  else
    echo "No supported package manager (dnf/apt-get) found" >&2; exit 1
  fi
  sudo systemctl enable --now docker
  sudo usermod -aG docker "$(whoami)" || true
fi
if ! sudo swapon --show 2>/dev/null | grep -q /swapfile; then
  sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
fi

# Build the single container image.
sudo docker build -t hmis-qa -f deploy/Dockerfile .

# (Re)start it. Data is wiped on every rebuild — intended for QA.
sudo docker rm -f hmis-qa 2>/dev/null || true
sudo docker run -d --name hmis-qa --restart unless-stopped -p 80:80 \
  -e HMIS_SECURITY_JWT_SECRET="${JWT:-qa-please-change-this-32char-minimum!!}" \
  -e HMIS_BOOTSTRAP_ROOT_PASSWORD="${ROOT_PW:-QaRoot!123}" \
  hmis-qa

echo "Container started. Recent logs:"
sleep 3
sudo docker logs --tail 20 hmis-qa || true
