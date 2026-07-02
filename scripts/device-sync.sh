#!/usr/bin/env bash
# Fast incremental build + deploy + relaunch for emulator/device development.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="${ROOT_DIR}/android"
APP_ID="${APP_ID:-com.bandu.tiji}"
APP_ACTIVITY="${APP_ACTIVITY:-${APP_ID}/.MainActivity}"
DEBUG_APK="${DEBUG_APK:-${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk}"
PRIVAPP_DIR="${PRIVAPP_DIR:-/system/priv-app/BanduTiji}"
PRIVAPP_APK="${PRIVAPP_APK:-${PRIVAPP_DIR}/BanduTiji.apk}"
PERM_XML="${PERM_XML:-${ROOT_DIR}/permissions/privapp-permissions-${APP_ID}.xml}"

SYNC_SKIP_BUILD="${SYNC_SKIP_BUILD:-0}"
SYNC_RELAUNCH="${SYNC_RELAUNCH:-1}"
SYNC_FAST_REBOOT="${SYNC_FAST_REBOOT:-1}"
SYNC_PRIVAPP="${SYNC_PRIVAPP:-0}"
SYNC_REMOTE_ADB="${SYNC_REMOTE_ADB:-1}"
REMOTE_ADB_PORT="${REMOTE_ADB_PORT:-5555}"

if [[ "${SYNC_NO_RELAUNCH:-0}" == "1" ]]; then
  SYNC_RELAUNCH=0
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
    die "no adb device/emulator connected; start one or set ADB_SERIAL"
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

wait_for_boot() {
  echo "INFO: waiting for ${SERIAL} to boot" >&2
  adb_cmd wait-for-device

  local booted=""
  for _ in {1..120}; do
    booted="$(adb_cmd shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    [[ "${booted}" == "1" ]] && break
    sleep 1
  done
  [[ "${booted}" == "1" ]] || die "device did not finish booting within 120 seconds"

  local resolved=""
  for _ in {1..30}; do
    if adb_cmd shell pm path "${APP_ID}" >/dev/null 2>&1; then
      resolved="$(adb_cmd shell cmd package resolve-activity --brief \
        -a android.intent.action.MAIN \
        -c android.intent.category.LAUNCHER \
        "${APP_ID}" 2>/dev/null | tr -d '\r' || true)"
      if echo "${resolved}" | grep -q "${APP_ID}/"; then
        return 0
      fi
    fi
    sleep 1
  done
  die "package manager did not expose ${APP_ID} launcher after boot"
}

is_privapp_install() {
  local path
  path="$(adb_cmd shell pm path "${APP_ID}" 2>/dev/null | head -n1 | tr -d '\r' || true)"
  [[ -n "${path}" ]] || return 1
  [[ "${path}" == *"${PRIVAPP_DIR}"* ]] || [[ "${path}" == *"/system/priv-app/"* ]]
}

build_debug_apk() {
  echo "== incremental assembleDebug" >&2
  (
    cd "${ANDROID_DIR}"
    ./gradlew :app:assembleDebug \
      --parallel \
      --build-cache \
      -x test \
      -x lint
  )
  [[ -f "${DEBUG_APK}" ]] || die "APK not found after build: ${DEBUG_APK}"
}

install_user_apk() {
  echo "== install debug APK (${SERIAL})" >&2
  adb_cmd install -r -d "${DEBUG_APK}" >/dev/null
}

clear_user_update() {
  adb_cmd shell pm uninstall-system-updates "${APP_ID}" >/dev/null 2>&1 || true
  adb_cmd shell cmd package uninstall-system-updates "${APP_ID}" >/dev/null 2>&1 || true
}

ensure_root_remount() {
  echo "== adb root + remount" >&2
  adb_cmd root >/dev/null 2>&1 || true
  sleep 1
  adb_cmd wait-for-device

  local remount_output
  remount_output="$(adb_cmd remount 2>&1 || true)"
  if ! echo "${remount_output}" | grep -qiE 'remount succeeded|using overlayfs'; then
    printf "%s\n" "${remount_output}" >&2
    die "adb remount did not succeed (priv-app sync requires writable system)"
  fi
}

push_privapp_apk() {
  clear_user_update
  echo "== push APK to ${PRIVAPP_APK}" >&2
  adb_cmd shell mkdir -p "${PRIVAPP_DIR}"
  adb_cmd push "${DEBUG_APK}" "${PRIVAPP_APK}" >/dev/null
  adb_cmd shell chmod 0644 "${PRIVAPP_APK}"
  adb_cmd shell chown root:root "${PRIVAPP_APK}" >/dev/null 2>&1 || true
  adb_cmd shell restorecon "${PRIVAPP_APK}" >/dev/null 2>&1 || true
}

push_privapp_permissions() {
  [[ -f "${PERM_XML}" ]] || die "permission XML not found: ${PERM_XML}"
  local remote="/system/etc/permissions/$(basename "${PERM_XML}")"
  echo "== push privapp permissions XML to ${remote}" >&2
  adb_cmd push "${PERM_XML}" "${remote}" >/dev/null
  adb_cmd shell chmod 0644 "${remote}"
  adb_cmd shell chown root:root "${remote}" >/dev/null 2>&1 || true
  adb_cmd shell restorecon "${remote}" >/dev/null 2>&1 || true
}

reboot_device() {
  if [[ "${SYNC_FAST_REBOOT}" == "1" ]]; then
    echo "== soft reboot (stop/start)" >&2
    adb_cmd shell stop >/dev/null 2>&1 || true
    sleep 1
    adb_cmd shell start >/dev/null 2>&1 || true
  else
    echo "== reboot device" >&2
    adb_cmd reboot
  fi
  wait_for_boot
}

relaunch_app() {
  echo "== relaunch ${APP_ID}" >&2
  adb_cmd shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true

  local out=""
  for attempt in {1..15}; do
    out="$(adb_cmd shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "${APP_ACTIVITY}" 2>&1 | tr -d '\r' || true)"
    if ! echo "${out}" | grep -qiE 'Too early to start activity|Error type 3|Activity class .* does not exist|Unable to resolve'; then
      break
    fi
    echo "INFO: activity manager not ready; retrying launch (${attempt}/15)" >&2
    sleep 1
  done
  [[ -n "${out}" ]] && echo "${out}" >&2
  if echo "${out}" | grep -qiE '(^Error|error type|Activity class .* does not exist|Unable to resolve)'; then
    echo "WARN: explicit activity launch failed; falling back to monkey" >&2
    adb_cmd shell monkey -p "${APP_ID}" -c android.intent.category.LAUNCHER 1 >/dev/null
  fi
}

enable_emulator_remote_adb() {
  [[ "${SYNC_REMOTE_ADB}" == "1" ]] || return 0
  [[ "${SERIAL}" == emulator-* ]] || return 0
  echo "== enable emulator TCP adb (:${REMOTE_ADB_PORT})" >&2
  if ! adb_cmd tcpip "${REMOTE_ADB_PORT}" >/dev/null 2>&1; then
    echo "WARN: adb tcpip ${REMOTE_ADB_PORT} failed; remote ADB status may stay disabled" >&2
    return 0
  fi
  sleep 1
  adb_cmd wait-for-device >/dev/null 2>&1 || true
}

SDK_ROOT="$(resolve_sdk_root)" || die "ANDROID_SDK_ROOT/ANDROID_HOME is not set and ${HOME}/Library/Android/sdk was not found"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found under ${SDK_ROOT}"
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

if [[ "${SYNC_PRIVAPP}" == "1" ]] || is_privapp_install; then
  if [[ "${SYNC_PRIVAPP}" == "1" ]]; then
    echo "INFO: forcing system priv-app sync (SYNC_PRIVAPP=1)" >&2
  else
    echo "INFO: detected system priv-app install" >&2
  fi
  ensure_root_remount
  push_privapp_apk
  push_privapp_permissions
  reboot_device
elif adb_cmd shell pm path "${APP_ID}" >/dev/null 2>&1; then
  install_user_apk
else
  echo "INFO: app not installed; installing debug APK" >&2
  install_user_apk
fi

enable_emulator_remote_adb

if [[ "${SYNC_RELAUNCH}" == "1" ]]; then
  relaunch_app
fi

echo "INFO: sync complete (${SERIAL})" >&2
