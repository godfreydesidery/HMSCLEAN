# QA deployment — quick reference

> Full breakdown of every step is in [`../DEPLOY_QA.md`](../DEPLOY_QA.md). This is
> the short version that lives next to the scripts.

## The commands (run from repo root, in Git Bash, AWS CLI configured)

```bash
bash deploy/aws-up.sh         # INITIAL: create EC2 + build & run the container
bash deploy/aws-redeploy.sh   # CONTINUOUS: re-sync + rebuild + restart same box
bash deploy/aws-down.sh       # teardown (terminate + delete SG/key)
```

`aws-up.sh` provisions the key pair, security group (SSH/HTTP from your IP), and a
`t3.medium` Amazon Linux 2023 instance, saves its details to `deploy/.qa-state`,
then hands off to `aws-redeploy.sh` which ships the code and builds/runs the single
container. Both print the URL + `root` / `QaRoot!123` login when finished.

Override anything via env:

```bash
AWS_REGION=eu-west-1 INSTANCE_TYPE=t3.large HMIS_BOOTSTRAP_ROOT_PASSWORD='Secret!123' bash deploy/aws-up.sh
```

## What's under `deploy/`

- `Dockerfile`, `nginx.conf`, `start.sh` — the single-container image (Postgres + Spring Boot + nginx)
- `aws-up.sh` / `aws-redeploy.sh` / `aws-down.sh` — laptop orchestrators
- `remote-build.sh` — runs on the instance (installs Docker + swap, builds, runs)

Two robustness fixes so it works from a Windows machine:

- **Sync without rsync**: `aws-redeploy.sh` uses `rsync` if present, else falls back
  to `tar`-over-SSH (Git Bash has `tar`).
- **`.gitattributes`** pins the scripts to **LF** — `core.autocrlf=true` would
  otherwise rewrite them as CRLF and break the shebang on Linux.

## Credentials & secrets

- **AWS credentials (required):** `aws configure` once — the scripts read
  `~/.aws/`; never put AWS keys in the repo. The identity needs EC2
  create/destroy + `ssm:GetParameters` (simplest: `AmazonEC2FullAccess` +
  `AmazonSSMReadOnlyAccess`).
- **SSH key (`.pem`):** auto-created by `aws-up.sh` as `hmis-qa-key.pem` **in the
  repo root** (`chmod 400`, gitignored). Leave it there — the scripts expect it
  at the repo root. `aws-down.sh` deletes it.
- **App secrets (optional):** `HMIS_SECURITY_JWT_SECRET` (≥ 32 chars) and
  `HMIS_BOOTSTRAP_ROOT_PASSWORD` (default `QaRoot!123`) — pass as env to override.
- **Not needed:** Docker-registry login or GitHub token.

Full detail: the **Credentials & secrets** section of [`../DEPLOY_QA.md`](../DEPLOY_QA.md).

## Prerequisites

AWS CLI v2 configured, plus `ssh` (Git Bash has it). The `.pem` key and
`deploy/.qa-state` are gitignored. The whole thing is throwaway — `aws-down.sh`
removes everything.
