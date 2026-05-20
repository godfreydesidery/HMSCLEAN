#!/usr/bin/env bash
# TEARDOWN: terminate the QA instance and remove the security group + key pair.
# Run from the repo root: bash deploy/aws-down.sh
set -euo pipefail
cd "$(dirname "$0")/.."

STATE="deploy/.qa-state"
[ -f "$STATE" ] || { echo "No $STATE — nothing to tear down."; exit 0; }
# shellcheck disable=SC1090
source "$STATE"

echo "==> Terminating $IID …"
aws ec2 terminate-instances --region "$AWS_REGION" --instance-ids "$IID" >/dev/null
aws ec2 wait instance-terminated --region "$AWS_REGION" --instance-ids "$IID"

echo "==> Deleting security group $SG_ID …"
aws ec2 delete-security-group --region "$AWS_REGION" --group-id "$SG_ID" 2>/dev/null || true

echo "==> Deleting key pair $KEY …"
aws ec2 delete-key-pair --region "$AWS_REGION" --key-name "$KEY" 2>/dev/null || true
rm -f "$KEY.pem" "$STATE"

echo "==> Torn down."
