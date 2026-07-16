#!/usr/bin/env bash

# Shared API endpoint policy for Flutter build and device sync scripts.
: "${EMULATOR_API_BASE_URL:=http://10.0.2.2:3000/api/mobile/v1}"
: "${DEVICE_API_BASE_URL:=http://100.69.41.14:3000/api/mobile/v1}"
: "${PROD_API_BASE_URL:=https://aibandu.dpdns.org/api/mobile/v1}"
: "${RELEASE_API_BASE_URL:=${PROD_API_BASE_URL}}"

api_url_is_emulator_only() {
  local url="${1:-}"
  case "${url}" in
    http://10.0.2.2|http://10.0.2.2/*|http://10.0.2.2:*|\
    https://10.0.2.2|https://10.0.2.2/*|https://10.0.2.2:*|\
    http://127.0.0.1|http://127.0.0.1/*|http://127.0.0.1:*|\
    https://127.0.0.1|https://127.0.0.1/*|https://127.0.0.1:*|\
    http://localhost|http://localhost/*|http://localhost:*|\
    https://localhost|https://localhost/*|https://localhost:*)
      return 0
      ;;
  esac
  return 1
}

verify_production_api_base_url() {
  local url="${1:-}"
  local bypass_auth="${2:-false}"

  if [[ "${url}" != "${PROD_API_BASE_URL}" ]]; then
    echo "ERROR: production API must be exactly ${PROD_API_BASE_URL}; got ${url}" >&2
    return 1
  fi
  if [[ "${bypass_auth}" != "false" ]]; then
    echo "ERROR: production builds require BYPASS_AUTH=false" >&2
    return 1
  fi
}

resolve_api_base_url() {
  local target="${1:-}"
  local requested="${API_BASE_URL:-}"
  local resolved=""

  case "${target}" in
    emulator)
      resolved="${requested:-${EMULATOR_API_BASE_URL}}"
      ;;
    device)
      if [[ -z "${requested}" ]]; then
        resolved="${DEVICE_API_BASE_URL}"
      elif api_url_is_emulator_only "${requested}"; then
        echo "WARN: ${requested} is emulator-only; switching physical device to ${DEVICE_API_BASE_URL}" >&2
        resolved="${DEVICE_API_BASE_URL}"
      else
        resolved="${requested}"
      fi
      if [[ -z "${resolved}" ]] || api_url_is_emulator_only "${resolved}"; then
        echo "ERROR: physical-device debug API must be explicitly configured" >&2
        return 1
      fi
      ;;
    release|production)
      resolved="${requested:-${RELEASE_API_BASE_URL}}"
      verify_production_api_base_url "${resolved}" "${BYPASS_AUTH:-false}" || return 1
      ;;
    *)
      echo "ERROR: unknown API target '${target}'; expected emulator, device, or release" >&2
      return 1
      ;;
  esac

  printf '%s\n' "${resolved}"
}
