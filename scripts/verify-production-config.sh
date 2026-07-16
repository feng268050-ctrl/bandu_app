#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROD_API_BASE_URL="${PROD_API_BASE_URL:-https://aibandu.dpdns.org/api/mobile/v1}"
BYPASS_AUTH="${BYPASS_AUTH:-false}"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/api-base-url.sh"
verify_production_api_base_url "${PROD_API_BASE_URL}" "${BYPASS_AUTH}"

if rg -n 'api\.aibandu\.dpdns\.org' \
  "${ROOT_DIR}/lib" \
  "${ROOT_DIR}/android" \
  "${ROOT_DIR}/scripts" \
  "${ROOT_DIR}/Makefile" \
  "${ROOT_DIR}/README.md" \
  "${ROOT_DIR}/.env.example"; then
  echo "ERROR: deprecated API subdomain found in production sources" >&2
  exit 1
fi

if rg -n 'DioException|SocketException' \
  "${ROOT_DIR}/lib/application" \
  "${ROOT_DIR}/lib/components"; then
  echo "ERROR: transport exceptions must not reach user-facing layers" >&2
  exit 1
fi

if rg -n 'usesCleartextTraffic="true"' \
  "${ROOT_DIR}/android/app/src/main/AndroidManifest.xml" \
  "${ROOT_DIR}/android/app/src/profile/AndroidManifest.xml"; then
  echo "ERROR: cleartext traffic is only allowed in the debug manifest" >&2
  exit 1
fi

if rg -n 'signingConfigs\.getByName\("debug"\)' \
  "${ROOT_DIR}/android/app/build.gradle.kts"; then
  echo "ERROR: release builds must not use the debug signing key" >&2
  exit 1
fi

if ! rg -q '^android/key\.properties$' "${ROOT_DIR}/.gitignore"; then
  echo "ERROR: android/key.properties must be ignored by Git" >&2
  exit 1
fi

echo "Production configuration verified: ${PROD_API_BASE_URL}"
