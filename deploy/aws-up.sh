#!/usr/bin/env bash
# INITIAL deploy: create the AWS key pair + security group + EC2 instance, then
# build & run the single QA container on it. Run from the repo root:
#     bash deploy/aws-up.sh
#
# Tunables (env): AWS_REGION (us-east-1), KEY (hmis-qa-key), SG (hmis-qa-sg),
#                 INSTANCE_TYPE (t3.medium), HMIS_BOOTSTRAP_ROOT_PASSWORD, HMIS_SECURITY_JWT_SECRET
# Requires: aws CLI v2 (configured), ssh, and tar or rsync.
set -euo pipefail
cd "$(dirname "$0")/.."

AWS_REGION="${AWS_REGION:-us-east-1}"
KEY="${KEY:-hmis-qa-key}"
SG="${SG:-hmis-qa-sg}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.medium}"
STATE="deploy/.qa-state"

echo "==> Region: $AWS_REGION   Instance type: $INSTANCE_TYPE"

# 1. Key pair (kept locally; gitignored).
if [ ! -f "$KEY.pem" ]; then
  aws ec2 create-key-pair --region "$AWS_REGION" --key-name "$KEY" \
    --query KeyMaterial --output text > "$KEY.pem"
  chmod 400 "$KEY.pem"
  echo "==> Created key pair -> $KEY.pem"
fi

# 2. Security group (create if missing) + ingress for SSH/HTTP from your IP.
SG_ID=$(aws ec2 describe-security-groups --region "$AWS_REGION" \
  --group-names "$SG" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)
if [ -z "$SG_ID" ] || [ "$SG_ID" = "None" ]; then
  SG_ID=$(aws ec2 create-security-group --region "$AWS_REGION" \
    --group-name "$SG" --description "HMIS QA" --query GroupId --output text)
  echo "==> Created security group $SG_ID"
fi
MYIP="$(curl -s https://checkip.amazonaws.com)/32"
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges="[{CidrIp=$MYIP}]" 2>/dev/null || true
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --ip-permissions IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=$MYIP}]" 2>/dev/null || true
echo "==> Ingress allowed for $MYIP (SSH 22, HTTP 80)"

# 3. Launch instance (Amazon Linux 2023, 30 GB gp3).
IID=$(aws ec2 run-instances --region "$AWS_REGION" \
  --image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --instance-type "$INSTANCE_TYPE" \
  --key-name "$KEY" --security-group-ids "$SG_ID" \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3}' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=hmis-qa}]' \
  --query 'Instances[0].InstanceId' --output text)
echo "==> Launched $IID — waiting for running state …"
aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$IID"
PUBDNS=$(aws ec2 describe-instances --region "$AWS_REGION" --instance-ids "$IID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)

# 4. Persist state for redeploy / teardown.
cat > "$STATE" <<EOF
AWS_REGION=$AWS_REGION
KEY=$KEY
SG=$SG
SG_ID=$SG_ID
IID=$IID
PUBDNS=$PUBDNS
EOF
echo "==> Instance $IID at $PUBDNS"

# 5. Wait for SSH to answer.
echo "==> Waiting for SSH …"
for _ in $(seq 1 40); do
  if ssh -i "$KEY.pem" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=5 \
      ec2-user@"$PUBDNS" 'echo ok' >/dev/null 2>&1; then break; fi
  sleep 5
done

# 6. Ship code + build + run (reuses the continuous-deploy script).
exec bash deploy/aws-redeploy.sh
