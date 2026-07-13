#!/usr/bin/env bash
# Start (or reuse) an Android emulator for local Flutter development.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AVD="${AVD:-Bandu_Pixel_7_Pro_API_36}"
EMULATOR_API_LEVEL="${EMULATOR_API_LEVEL:-36}"
EMULATOR_PORT="${EMULATOR_PORT:-5556}"
SERIAL="${ADB_SERIAL:-emulator-${EMULATOR_PORT}}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
  SERIAL="${ADB_SERIAL:-emulator-${EMULATOR_PORT}}"
fi

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
    [[ -f "${dir}/system.img" ]] && { echo "system-images;android-${api};google_apis/${abi}"; return 0; }
  done
  return 1
}

wait_for_boot() {
  echo "INFO: waiting for ${SERIAL}" >&2
  "${ADB_BIN}" -s "${SERIAL}" wait-for-device

  local booted=""
  for _ in {1..180}; do
    booted="$("${ADB_BIN}" -s "${SERIAL}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    [[ "${booted}" == "1" ]] && return 0
    sleep 1
  done
  die "device did not finish booting within 180 seconds"
}

ensure_avd() {
  if "${EMULATOR_BIN}" -list-avds | grep -Fxq "${AVD}"; then
    return 0
  fi

  local package_key
  package_key="$(find_system_image "${SDK_ROOT}" "${EMULATOR_API_LEVEL}")" \
    || die "No Android ${EMULATOR_API_LEVEL} emulator system image found."

  echo "INFO: creating AVD ${AVD} from ${package_key}" >&2
  if ! printf "no\n" | "${AVDMANAGER_BIN}" create avd -n "${AVD}" -k "${package_key}" -d pixel_7_pro >/dev/null; then
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

is_emulator_online() {
  "${ADB_BIN}" devices 2>/dev/null | awk -v s="${SERIAL}" '$1 == s && $2 == "device" { found = 1 } END { exit !found }'
}

is_headless_emulator() {
  local line=""
  line="$(ps aux 2>/dev/null | grep -E "[q]emu-system.*-port[[:space:]]+${EMULATOR_PORT}" || true)"
  [[ -n "${line}" ]] || return 1
  echo "${line}" | grep -qE '(-no-window|qemu-system-[^ ]*-headless)'
}

avd_launch_params_file() {
  echo "${HOME}/.android/avd/${AVD}.avd/emu-launch-params.txt"
}

has_headless_launch_params() {
  local params_file
  params_file="$(avd_launch_params_file)"
  [[ -f "${params_file}" ]] || return 1
  grep -qE '^-no-window$' "${params_file}"
}

write_launch_params() {
  local avd_dir="${HOME}/.android/avd/${AVD}.avd"
  local params_file
  params_file="$(avd_launch_params_file)"
  [[ -d "${avd_dir}" ]] || return 0

  local -a args=()
  args+=(-avd "${AVD}" -no-snapshot-load -port "${EMULATOR_PORT}")
  [[ -n "${EMULATOR_GPU:-}" ]] && args+=(-gpu "${EMULATOR_GPU}")
  [[ -n "${EMULATOR_SCALE:-}" ]] && args+=(-scale "${EMULATOR_SCALE}")
  [[ "${EMULATOR_NO_AUDIO:-0}" == "1" ]] && args+=(-no-audio)
  [[ "${EMULATOR_WRITABLE_SYSTEM:-0}" == "1" ]] && args+=(-writable-system)
  [[ "${EMULATOR_HEADLESS:-0}" == "1" ]] && args+=(-no-window)

  local argc=$(( ${#args[@]} + 1 ))
  {
    printf '%s\n' "${ROOT_DIR}"
    printf '%s\n' "${EMULATOR_BIN}"
    printf '%s\n' "${argc}"
    printf '%s\n' "${EMULATOR_BIN}"
    printf '%s\n' "${args[@]}"
  } > "${params_file}"
}

stop_emulator() {
  if is_emulator_online; then
    echo "INFO: stopping ${SERIAL}" >&2
    "${ADB_BIN}" -s "${SERIAL}" emu kill >/dev/null 2>&1 || true
  fi

  local pid=""
  while IFS= read -r pid; do
    [[ -n "${pid}" ]] || continue
    kill "${pid}" >/dev/null 2>&1 || true
  done < <(ps aux 2>/dev/null | grep -E "[q]emu-system.*(-port[[:space:]]+${EMULATOR_PORT}|[[:space:]]${AVD})" | awk '{print $2}')

  for _ in {1..60}; do
    if ! ps aux 2>/dev/null | grep -qE "[q]emu-system.*(-port[[:space:]]+${EMULATOR_PORT}|[[:space:]]${AVD})"; then
      sleep 1
      return 0
    fi
    sleep 1
  done
  die "failed to stop emulator for ${AVD} on port ${EMULATOR_PORT}"
}

prepare_gui_launch() {
  stop_emulator
  write_launch_params
}

launch_emulator() {
  local emu_cmd=("${EMULATOR_BIN}" -avd "${AVD}" -no-snapshot-load -port "${EMULATOR_PORT}")
  [[ -n "${EMULATOR_GPU:-}" ]] && emu_cmd+=(-gpu "${EMULATOR_GPU}")
  [[ -n "${EMULATOR_SCALE:-}" ]] && emu_cmd+=(-scale "${EMULATOR_SCALE}")
  [[ "${EMULATOR_NO_AUDIO:-0}" == "1" ]] && emu_cmd+=(-no-audio)
  [[ "${EMULATOR_WRITABLE_SYSTEM:-0}" == "1" ]] && emu_cmd+=(-writable-system)
  [[ "${EMULATOR_HEADLESS:-0}" == "1" ]] && emu_cmd+=(-no-window)

  printf "INFO: emulator command:" >&2
  printf " %q" "${emu_cmd[@]}" >&2
  printf "\n" >&2

  "${ADB_BIN}" start-server >/dev/null
  if [[ "${EMULATOR_HEADLESS:-0}" == "1" ]]; then
    "${emu_cmd[@]}" >/dev/null 2>&1 &
  else
    "${emu_cmd[@]}" &
  fi
  EMULATOR_PID=$!
  echo "${EMULATOR_PID}" > "${ROOT_DIR}/.emulator.pid"
}

SDK_ROOT="$(resolve_sdk_root)" || die "ANDROID_SDK_ROOT/ANDROID_HOME not found"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found"
EMULATOR_BIN="$(resolve_tool "${SDK_ROOT}" emulator)" || die "emulator not found"
AVDMANAGER_BIN="$(resolve_tool "${SDK_ROOT}" avdmanager)" || die "avdmanager not found"

if [[ "${EMULATOR_RECREATE:-0}" == "1" || "${REBUILD_IMAGE:-0}" == "1" ]]; then
  remove_avd_for_fresh_create
fi

if is_emulator_online; then
  if [[ "${EMULATOR_HEADLESS:-0}" != "1" ]] && { is_headless_emulator || has_headless_launch_params; }; then
    echo "INFO: ${SERIAL} is headless (cached -no-window); restarting with GUI window" >&2
    prepare_gui_launch
    ensure_avd
    launch_emulator
    wait_for_boot
    echo "INFO: emulator started on ${SERIAL} (pid=${EMULATOR_PID:-unknown})" >&2
  else
    echo "INFO: ${SERIAL} already online" >&2
  fi
else
  if [[ "${EMULATOR_HEADLESS:-0}" != "1" ]]; then
    if has_headless_launch_params || ps aux 2>/dev/null | grep -qE "[q]emu-system.*(-port[[:space:]]+${EMULATOR_PORT}|[[:space:]]${AVD})"; then
      echo "INFO: clearing cached headless launch params for ${AVD}" >&2
      prepare_gui_launch
    else
      write_launch_params
    fi
  else
    write_launch_params
  fi
  ensure_avd
  launch_emulator
  wait_for_boot
  echo "INFO: emulator started on ${SERIAL} (pid=${EMULATOR_PID:-unknown})" >&2
fi

export ADB_SERIAL="${SERIAL}"
export ANDROID_SERIAL="${SERIAL}"
echo "${SERIAL}"
