#!/usr/bin/env bash
set -euo pipefail
cd /opt/dasibom
set -a
source .env.production
set +a
# The admin endpoint is only bound to localhost and blocked by the public proxy.
curl --fail --silent --show-error -X POST -H "Authorization: Bearer $ADMIN_TOKEN" http://127.0.0.1:8080/api/v1/admin/sync/catalog
