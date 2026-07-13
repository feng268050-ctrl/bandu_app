#!/usr/bin/env bash
# Incremental debug build + install + relaunch for emulator/device.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="${APP_ID:-com.bandu.tiji}"
APP_ACTIVITY="${APP_ACTIVITY:-${APP_ID}/.MainActivity}"
DEBUG_APK="${DEBUG_APK:-${ROOT_DIR}/build/app/outputs/flutter-apk/app-debug.apk}"
API_BASE_URL="${API_BASE_URL:-http://10.0.2.2:3000/api/mobile/v1}"
APP_VERSION="${APP_VERSION:-v0.1.1}"
BYPASS_AUTH="${BYPASS_AUTH:-false}"
SYNC_SKIP_BUILD="${SYNC_SKIP_BUILD:-0}"
SYNC_RELAUNCH="${SYNC_RELAUNCH:-1}"

if [[ "${SYNC_NO_RELAUNCH:-0}" == "1" ]]; then
  SYNC_RELAUNCH=0
fi

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ -f "${ROOT_DIR}/VERSION" ]]; then
  APP_VERSION="$(tr -d '[:space:]' < "${ROOT_DIR}/VERSION")"
fi

die() {
  echo "ERROR: $*" >&2
  exit 1
}

resolve_sdk_root() {
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "${ANDROID_SDK_ROOT}" ]]; then
    echo "${ANDROID_SDK_ROOT}"
    return 0
  fi
  if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}" ]]; then
    echo "${ANDROID_HOME}"
    return 0
  fi
  if [[ -d "${HOME}/Library/Android/sdk" ]]; then
    echo "${HOME}/Library/Android/sdk"
    return 0
  fi
  return 1
}

resolve_tool() {
  local sdk="$1" name="$2"
  if command -v "${name}" >/dev/null 2>&1; then
    command -v "${name}"
    return 0
  fi
  for candidate in \
    "${sdk}/platform-tools/${name}" \
    "${sdk}/emulator/${name}" \
    "${sdk}/cmdline-tools/latest/bin/${name}" \
    "${sdk}/cmdline-tools/bin/${name}" \
    "${sdk}/tools/bin/${name}"; do
    [[ -x "${candidate}" ]] && { echo "${candidate}"; return 0; }
  done
  return 1
}

resolve_serial() {
  local serial="${ADB_SERIAL:-${ANDROID_SERIAL:-}}"
  if [[ -n "${serial}" ]]; then
    echo "${serial}"
    return 0
  fi

  local devices=()
  while IFS= read -r line; do
    [[ -n "${line}" ]] && devices+=("${line}")
  done < <("${ADB_BIN}" devices 2>/dev/null | awk 'NR > 1 && $2 == "device" { print $1 }')

  if [[ "${#devices[@]}" -eq 0 ]]; then
    die "no adb device/emulator connected; set ADB_SERIAL or run make emulator"
  fi
  if [[ "${#devices[@]}" -gt 1 ]]; then
    echo "ERROR: multiple adb targets connected:" >&2
    printf "  %s\n" "${devices[@]}" >&2
    die "set ADB_SERIAL to the target serial"
  fi
  echo "${devices[0]}"
}

adb_cmd() {
  "${ADB_BIN}" -s "${SERIAL}" "$@"
}

build_debug_apk() {
  echo "== flutter build apk --debug" >&2
  (
    cd "${ROOT_DIR}"
    flutter build apk --debug \
      --dart-define="API_BASE_URL=${API_BASE_URL}" \
      --dart-define="APP_VERSION=${APP_VERSION}" \
      --dart-define="BYPASS_AUTH=${BYPASS_AUTH}"
  )
  [[ -f "${DEBUG_APK}" ]] || die "debug APK not found: ${DEBUG_APK}"
}

install_debug_apk() {
  echo "== adb install debug APK (${SERIAL})" >&2
  adb_cmd install -r -d "${DEBUG_APK}" >/dev/null
}

relaunch_app() {
  echo "== relaunch ${APP_ID}" >&2
  adb_cmd shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true
  local out
  out="$(adb_cmd shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "${APP_ACTIVITY}" 2>&1 | tr -d '\r' || true)"
  [[ -n "${out}" ]] && echo "${out}" >&2
  if echo "${out}" | grep -qiE '(^Error|error type|Activity class .* does not exist|Unable to resolve)'; then
    echo "WARN: explicit activity launch failed; falling back to monkey" >&2
    adb_cmd shell monkey -p "${APP_ID}" -c android.intent.category.LAUNCHER 1 >/dev/null
  fi
}

SDK_ROOT="$(resolve_sdk_root)" || die "ANDROID_SDK_ROOT/ANDROID_HOME not found"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found"
SERIAL="$(resolve_serial)"
export ADB_SERIAL="${SERIAL}"
export ANDROID_SERIAL="${SERIAL}"

echo "INFO: sync target ${SERIAL}" >&2

if [[ "${SYNC_SKIP_BUILD}" != "1" ]]; then
  build_debug_apk
else
  [[ -f "${DEBUG_APK}" ]] || die "SYNC_SKIP_BUILD=1 but APK missing: ${DEBUG_APK}"
  echo "INFO: skipping build (SYNC_SKIP_BUILD=1)" >&2
fi

install_debug_apk

if [[ "${SYNC_RELAUNCH}" == "1" ]]; then
  relaunch_app
fi

echo "INFO: sync complete (${SERIAL})" >&2
