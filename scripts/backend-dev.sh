#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
if [ -f .env ]; then set -a; source .env; set +a; fi
if [ -z "${TOUR_API_SERVICE_KEY:-}" ] && [ -t 0 ]; then
  read -r -s -p "TourAPI key (Enter to skip): " TOUR_API_SERVICE_KEY
  echo
  export TOUR_API_SERVICE_KEY
fi
exec ./gradlew bootRun
