#!/usr/bin/env bash
# CONTINUOUS deploy: sync the latest source to the existing QA instance and
# rebuild/restart the container. Run from the repo root: bash deploy/aws-redeploy.sh
set -euo pipefail
cd "$(dirname "$0")/.."

STATE="deploy/.qa-state"
[ -f "$STATE" ] || { echo "No $STATE — run deploy/aws-up.sh first."; exit 1; }
# shellcheck disable=SC1090
source "$STATE"

ROOT_PW="${HMIS_BOOTSTRAP_ROOT_PASSWORD:-QaRoot!123}"
JWT="${HMIS_SECURITY_JWT_SECRET:-qa-please-change-this-32char-minimum!!}"
SSH="ssh -i $KEY.pem -o StrictHostKeyChecking=accept-new"

echo "==> Syncing source to $PUBDNS …"
if command -v rsync >/dev/null 2>&1; then
  rsync -az --delete -e "$SSH" \
    --exclude '.git' --exclude 'node_modules' --exclude 'target' \
    --exclude 'dist' --exclude '*.pem' --exclude 'deploy/.qa-state' \
    ./ ec2-user@"$PUBDNS":~/hmis/
else
  # rsync-free fallback (works in Git Bash on Windows): tar over SSH
  tar czf - --exclude=.git --exclude=node_modules --exclude=target \
            --exclude=dist --exclude='*.pem' --exclude=deploy/.qa-state . \
    | $SSH ec2-user@"$PUBDNS" 'rm -rf ~/hmis && mkdir -p ~/hmis && tar xzf - -C ~/hmis'
fi

echo "==> Building & running the container on the instance …"
$SSH ec2-user@"$PUBDNS" "ROOT_PW='$ROOT_PW' JWT='$JWT' bash ~/hmis/deploy/remote-build.sh"

echo
echo "============================================================"
echo " QA app:  http://$PUBDNS"
echo " Login:   root / $ROOT_PW   (forced change on first login)"
echo "============================================================"
