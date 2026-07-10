#!/usr/bin/env bash
# Legacy Android (Compose) client: build debug APK and install on a connected phone.
# For the new Flutter mobile client, use scripts/flutter-device.sh or `make install-app`.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="${ROOT_DIR}/android"
APP_ID="${APP_ID:-com.bandu.tiji}"
APP_ACTIVITY="${APP_ACTIVITY:-${APP_ID}/.MainActivity}"
DEBUG_APK="${DEBUG_APK:-${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk}"

INSTALL_SKIP_BUILD="${INSTALL_SKIP_BUILD:-0}"
INSTALL_RELAUNCH="${INSTALL_RELAUNCH:-1}"

if [[ "${INSTALL_NO_RELAUNCH:-0}" == "1" ]]; then
  INSTALL_RELAUNCH=0
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
  if [[ -n "${LOCALAPPDATA:-}" && -d "${LOCALAPPDATA}/Android/Sdk" ]]; then
    echo "${LOCALAPPDATA}/Android/Sdk"
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
    "${sdk}/platform-tools/${name}.exe"; do
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
    die "no adb device connected; plug in a phone, enable USB debugging, and approve the prompt"
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
  echo "== assembleDebug" >&2
  (
    cd "${ANDROID_DIR}"
    if [[ -f "./gradlew" ]]; then
      ./gradlew :app:assembleDebug \
        --parallel \
        --build-cache \
        -x test \
        -x lint
    else
      ./gradlew.bat :app:assembleDebug \
        --parallel \
        --build-cache \
        -x test \
        -x lint
    fi
  )
  [[ -f "${DEBUG_APK}" ]] || die "APK not found after build: ${DEBUG_APK}"
}

install_user_apk() {
  echo "== install debug APK (${SERIAL})" >&2
  local out=""
  if ! out="$(adb_cmd install -r -d "${DEBUG_APK}" 2>&1)"; then
    if echo "${out}" | grep -qiE 'INSTALL_FAILED_UPDATE_INCOMPATIBLE|signatures do not match'; then
      echo "INFO: signature mismatch; uninstalling existing app" >&2
      adb_cmd uninstall "${APP_ID}" >/dev/null || true
      adb_cmd install -r -d "${DEBUG_APK}"
      return 0
    fi
    echo "${out}" >&2
    die "adb install failed"
  fi
}

relaunch_app() {
  echo "== relaunch ${APP_ID}" >&2
  adb_cmd shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true
  adb_cmd shell am start \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    -n "${APP_ACTIVITY}" >/dev/null
}

SDK_ROOT="$(resolve_sdk_root)" || die "ANDROID_SDK_ROOT/ANDROID_HOME is not set and no default SDK path was found"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found under ${SDK_ROOT}"
SERIAL="$(resolve_serial)"
export ADB_SERIAL="${SERIAL}"
export ANDROID_SERIAL="${SERIAL}"

echo "INFO: install target ${SERIAL}" >&2

if [[ "${INSTALL_SKIP_BUILD}" != "1" ]]; then
  build_debug_apk
else
  [[ -f "${DEBUG_APK}" ]] || die "INSTALL_SKIP_BUILD=1 but APK missing: ${DEBUG_APK}"
  echo "INFO: skipping build (INSTALL_SKIP_BUILD=1)" >&2
fi

install_user_apk

if [[ "${INSTALL_RELAUNCH}" == "1" ]]; then
  relaunch_app
fi

echo "INFO: install complete (${SERIAL})" >&2
