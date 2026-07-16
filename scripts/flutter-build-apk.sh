#!/usr/bin/env bash
# Build an APK with an endpoint appropriate for emulator debug or device release.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_MODE="${1:-release}"
API_BASE_URL_OVERRIDE="${API_BASE_URL:-}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ -n "${API_BASE_URL_OVERRIDE}" ]]; then
  API_BASE_URL="${API_BASE_URL_OVERRIDE}"
fi

if [[ -f "${ROOT_DIR}/VERSION" ]]; then
  APP_VERSION="$(tr -d '[:space:]' < "${ROOT_DIR}/VERSION")"
fi

APP_VERSION="${APP_VERSION:-v0.1.1}"
BYPASS_AUTH="${BYPASS_AUTH:-false}"
SKIP_RELEASE_API_PREFLIGHT="${SKIP_RELEASE_API_PREFLIGHT:-0}"
APP_ENV="${APP_ENV:-development}"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/api-base-url.sh"

case "${BUILD_MODE}" in
  debug)
    API_TARGET="emulator"
    ;;
  release)
    API_TARGET="release"
    APP_ENV="production"
    ;;
  *)
    echo "ERROR: unsupported APK build mode: ${BUILD_MODE}" >&2
    exit 1
    ;;
esac

RESOLVED_API_BASE_URL="$(resolve_api_base_url "${API_TARGET}")"
echo "INFO: ${BUILD_MODE} APK API_BASE_URL=${RESOLVED_API_BASE_URL}" >&2

if [[ "${BUILD_MODE}" == "release" && "${SKIP_RELEASE_API_PREFLIGHT}" != "1" ]]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: curl is required for the release API preflight" >&2
    exit 1
  fi

  PREFLIGHT_URL="https://aibandu.dpdns.org/api/health"
  HTTP_STATUS="$(
    curl --connect-timeout 5 --max-time 15 --silent --show-error \
      --output /dev/null --write-out '%{http_code}' "${PREFLIGHT_URL}" || true
  )"
  if [[ ! "${HTTP_STATUS}" =~ ^[234][0-9][0-9]$ ]]; then
    echo "ERROR: release API is unreachable: ${PREFLIGHT_URL}" >&2
    echo "ERROR: verify Cloudflare Tunnel and the public health endpoint" >&2
    echo "ERROR: use SKIP_RELEASE_API_PREFLIGHT=1 only for an intentional offline build" >&2
    exit 1
  fi
  echo "INFO: release API preflight status=${HTTP_STATUS}" >&2
fi

cd "${ROOT_DIR}"
flutter build apk "--${BUILD_MODE}" \
  --dart-define="APP_ENV=${APP_ENV}" \
  --dart-define="API_BASE_URL=${RESOLVED_API_BASE_URL}" \
  --dart-define="APP_VERSION=${APP_VERSION}" \
  --dart-define="BYPASS_AUTH=${BYPASS_AUTH}"
