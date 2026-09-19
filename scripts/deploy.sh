#!/usr/bin/env bash
set -euo pipefail
# Usage: SSH_KEY=/path/to/key.pem SITE_HOST=example.com ./scripts/deploy.sh ec2-user@host release-tag
target=${1:?EC2 SSH target required}
release=${2:?Release tag required}
: "${SSH_KEY:?SSH_KEY required}" "${SITE_HOST:?SITE_HOST required}"
[[ "$release" =~ ^[a-zA-Z0-9._-]+$ ]] || { echo 'Invalid release tag'; exit 1; }
cd "$(dirname "$0")/.."
(cd backend && ./gradlew --no-daemon test bootJar)
(cd frontend && npm ci && npm run lint && npm test && NEXT_PUBLIC_SITE_URL="https://$SITE_HOST" npm run build -- --webpack)
docker build --platform linux/amd64 -t "dasibom-backend:$release" backend
docker build --platform linux/amd64 -t "dasibom-frontend:$release" frontend
docker save "dasibom-backend:$release" "dasibom-frontend:$release" | gzip | ssh -i "$SSH_KEY" "$target" 'gunzip | sudo docker load'
scp -i "$SSH_KEY" infra/compose.production.yaml infra/Caddyfile "$target:/opt/dasibom/"
ssh -i "$SSH_KEY" "$target" "cd /opt/dasibom && sudo env RELEASE='$release' docker compose --env-file .env.production -f compose.production.yaml up -d --wait --wait-timeout 240"
curl --fail --retry 5 --retry-delay 3 "https://$SITE_HOST/api/health"
