#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="${ROOT_DIR}/android"
APP_ID="${APP_ID:-com.bandu.tiji}"
APP_ACTIVITY="${APP_ACTIVITY:-${APP_ID}/.MainActivity}"
PRIVAPP_DIR="${PRIVAPP_DIR:-/system/priv-app/BanduTiji}"
PRIVAPP_APK="${PRIVAPP_APK:-${PRIVAPP_DIR}/BanduTiji.apk}"
DEBUG_APK="${DEBUG_APK:-${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk}"
AVD="${AVD:-Bandu_Tiji_Tablet}"
EMULATOR_API_LEVEL="${EMULATOR_API_LEVEL:-36}"
EMULATOR_PORT="${EMULATOR_PORT:-5554}"
SERIAL="${ADB_SERIAL:-${ANDROID_SERIAL:-emulator-${EMULATOR_PORT}}}"

[[ "${AVD}" != */* ]] || {
  echo "ERROR: AVD must be a plain AVD name, not a path: ${AVD}" >&2
  exit 1
}

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

find_system_image() {
  local sdk="$1" api="$2"
  local abi
  for abi in arm64-v8a x86_64; do
    local dir="${sdk}/system-images/android-${api}/default/${abi}"
    [[ -f "${dir}/system.img" ]] && { echo "system-images;android-${api};default;${abi}"; return 0; }
    dir="${sdk}/system-images/android-${api}/google_apis/${abi}"
    [[ -f "${dir}/system.img" ]] && { echo "system-images;android-${api};google_apis;${abi}"; return 0; }
  done
  return 1
}

adb_cmd() {
  "${ADB_BIN}" -s "${SERIAL}" "$@"
}

wait_for_boot() {
  echo "INFO: waiting for ${SERIAL}" >&2
  "${ADB_BIN}" -s "${SERIAL}" wait-for-device

  local booted=""
  for _ in {1..180}; do
    booted="$(adb_cmd shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    [[ "${booted}" == "1" ]] && return 0
    sleep 1
  done
  die "emulator did not finish booting within 180 seconds"
}

ensure_avd() {
  if "${EMULATOR_BIN}" -list-avds | grep -Fxq "${AVD}"; then
    return 0
  fi

  local package_key
  package_key="$(find_system_image "${SDK_ROOT}" "${EMULATOR_API_LEVEL}")" \
    || die "No Android ${EMULATOR_API_LEVEL} emulator system image found. Install one with sdkmanager, then retry."

  echo "INFO: creating AVD ${AVD} from ${package_key}" >&2
  if ! printf "no\n" | "${AVDMANAGER_BIN}" create avd -n "${AVD}" -k "${package_key}" --device "pixel_tablet" >/dev/null; then
    echo "INFO: retrying AVD creation without explicit device profile" >&2
    printf "no\n" | "${AVDMANAGER_BIN}" create avd -n "${AVD}" -k "${package_key}" >/dev/null
  fi
}

remove_avd_for_fresh_create() {
  if "${ADB_BIN}" devices 2>/dev/null | awk -v s="${SERIAL}" '$1 == s { found = 1 } END { exit !found }'; then
    echo "INFO: stopping ${SERIAL} before recreating ${AVD}" >&2
    "${ADB_BIN}" -s "${SERIAL}" emu kill >/dev/null 2>&1 || true
    sleep 2
  fi

  echo "INFO: deleting AVD ${AVD}" >&2
  "${AVDMANAGER_BIN}" delete avd -n "${AVD}" >/dev/null 2>&1 || true
  rm -rf "${HOME}/.android/avd/${AVD}.avd"
  rm -f "${HOME}/.android/avd/${AVD}.ini"
}

launch_emulator() {
  local emu_cmd=("${EMULATOR_BIN}" -avd "${AVD}" -writable-system -no-snapshot-load -port "${EMULATOR_PORT}")
  [[ -n "${EMULATOR_GPU:-}" ]] && emu_cmd+=(-gpu "${EMULATOR_GPU}")
  [[ -n "${EMULATOR_SCALE:-}" ]] && emu_cmd+=(-scale "${EMULATOR_SCALE}")
  [[ "${EMULATOR_NO_AUDIO:-0}" == "1" ]] && emu_cmd+=(-no-audio)

  printf "INFO: emulator command:" >&2
  printf " %q" "${emu_cmd[@]}" >&2
  printf "\n" >&2

  "${ADB_BIN}" start-server >/dev/null
  "${emu_cmd[@]}" &
  EMULATOR_PID=$!
}

build_debug_apk() {
  echo "== build debug APK" >&2
  (
    cd "${ANDROID_DIR}"
    "${ANDROID_DIR}/gradlew" :app:assembleDebug
  )
  [[ -f "${DEBUG_APK}" ]] || die "APK not found after build: ${DEBUG_APK}"
}

root_and_remount() {
  echo "== adb root" >&2
  adb_cmd root || true
  sleep 1
  adb_cmd wait-for-device

  echo "== adb remount" >&2
  local remount_output
  remount_output="$(adb_cmd remount 2>&1 || true)"
  printf "%s\n" "${remount_output}" >&2

  if echo "${remount_output}" | grep -q "Now reboot your device for settings to take effect"; then
    echo "== reboot once for overlayfs/remount settings" >&2
    adb_cmd reboot
    wait_for_boot
    echo "== adb root after remount reboot" >&2
    adb_cmd root || true
    sleep 1
    adb_cmd wait-for-device
    echo "== adb remount after remount reboot" >&2
    adb_cmd remount
  elif ! echo "${remount_output}" | grep -qi "remount succeeded"; then
    die "adb remount did not succeed"
  fi
}

clear_user_update() {
  echo "== clear any stale system-app update: ${APP_ID}" >&2
  adb_cmd shell pm uninstall-system-updates "${APP_ID}" >/dev/null 2>&1 || true
  adb_cmd shell cmd package uninstall-system-updates "${APP_ID}" >/dev/null 2>&1 || true
}

push_privapp() {
  clear_user_update
  echo "== push APK to ${PRIVAPP_APK}" >&2
  adb_cmd shell mkdir -p "${PRIVAPP_DIR}"
  adb_cmd push "${DEBUG_APK}" "${PRIVAPP_APK}"
  adb_cmd shell chmod 0644 "${PRIVAPP_APK}"
  adb_cmd shell chown root:root "${PRIVAPP_APK}" >/dev/null 2>&1 || true
  adb_cmd shell restorecon "${PRIVAPP_APK}" >/dev/null 2>&1 || true
}

reboot_and_verify() {
  echo "== reboot after system app push" >&2
  adb_cmd reboot
  wait_for_boot

  echo "== verify package path" >&2
  local path dump
  path="$(adb_cmd shell pm path "${APP_ID}" 2>/dev/null | tr -d '\r' || true)"
  echo "${path}" >&2
  [[ -n "${path}" ]] || die "package not found after reboot: ${APP_ID}"

  if echo "${path}" | grep -q "package:${PRIVAPP_APK}"; then
    return 0
  fi

  dump="$(adb_cmd shell dumpsys package "${APP_ID}" 2>/dev/null | tr -d '\r' || true)"
  if echo "${dump}" | grep -q "PRIVATE_FLAG_PRIVILEGED\|privileged=true\|isPrivilegedApp=true"; then
    echo "WARN: pm path is not ${PRIVAPP_APK}, but dumpsys marks ${APP_ID} privileged." >&2
    return 0
  fi

  die "unexpected package path: ${path:-<empty>}"
}

start_app() {
  echo "== launch ${APP_ID}" >&2
  adb_cmd shell settings put system screen_off_timeout 2147483647 >/dev/null 2>&1 || true
  adb_cmd shell settings put global policy_control "immersive.full=${APP_ID}:*" >/dev/null 2>&1 \
    || adb_cmd shell settings put global policy_control "immersive.full=*" >/dev/null 2>&1 || true
  adb_cmd shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true

  local out
  out="$(adb_cmd shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "${APP_ACTIVITY}" 2>&1 | tr -d '\r' || true)"
  [[ -n "${out}" ]] && echo "${out}" >&2
  if echo "${out}" | grep -qiE '(^Error|error type|Activity class .* does not exist|Unable to resolve)'; then
    echo "WARN: explicit activity launch failed; falling back to monkey" >&2
    adb_cmd shell monkey -p "${APP_ID}" -c android.intent.category.LAUNCHER 1 >/dev/null
  fi
}

SDK_ROOT="$(resolve_sdk_root)" || die "ANDROID_SDK_ROOT/ANDROID_HOME is not set and ${HOME}/Library/Android/sdk was not found"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found under ${SDK_ROOT}"
EMULATOR_BIN="$(resolve_tool "${SDK_ROOT}" emulator)" || die "emulator binary not found under ${SDK_ROOT}"
AVDMANAGER_BIN="$(resolve_tool "${SDK_ROOT}" avdmanager)" || die "avdmanager not found under ${SDK_ROOT}"

if [[ "${EMULATOR_RECREATE:-0}" == "1" || "${REBUILD_IMAGE:-0}" == "1" ]]; then
  remove_avd_for_fresh_create
fi
ensure_avd
launch_emulator
export ADB_SERIAL="${SERIAL}"
export ANDROID_SERIAL="${SERIAL}"
wait_for_boot
build_debug_apk
root_and_remount
push_privapp
reboot_and_verify
start_app

echo "INFO: emulator is running (pid=${EMULATOR_PID}); close the emulator window to finish." >&2
wait "${EMULATOR_PID}"
