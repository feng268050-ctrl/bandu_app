#!/usr/bin/env bash
# Build and run the Flutter mobile client on a connected phone.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLUTTER_DIR="${FLUTTER_DIR:-${ROOT_DIR}/flutter_app}"
FLUTTER_PLATFORMS="${FLUTTER_PLATFORMS:-android}"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

resolve_flutter() {
  if command -v flutter >/dev/null 2>&1; then
    command -v flutter
    return 0
  fi
  return 1
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
    die "no adb device connected; plug in a phone and approve USB debugging"
  fi
  if [[ "${#devices[@]}" -gt 1 ]]; then
    echo "ERROR: multiple adb targets connected:" >&2
    printf "  %s\n" "${devices[@]}" >&2
    die "set ADB_SERIAL to the target serial"
  fi
  echo "${devices[0]}"
}

resolve_api_base_url() {
  if [[ -n "${API_BASE_URL:-}" ]]; then
    echo "${API_BASE_URL}"
    return 0
  fi
  if [[ -n "${MOBILE_API_BASE_URL:-}" ]]; then
    echo "${MOBILE_API_BASE_URL}"
    return 0
  fi
  if [[ -n "${NEXTAUTH_URL:-}" ]]; then
  local trimmed="${NEXTAUTH_URL%/}"
    echo "${trimmed}/api/mobile/v1"
    return 0
  fi
  die "set API_BASE_URL to a phone-reachable URL, e.g. http://192.168.1.10:3000/api/mobile/v1"
}

ensure_platforms() {
  if [[ -f "${FLUTTER_DIR}/android/app/build.gradle" || -f "${FLUTTER_DIR}/android/app/build.gradle.kts" ]]; then
    return 0
  fi
  echo "== flutter create --platforms=${FLUTTER_PLATFORMS}" >&2
  (
    cd "${FLUTTER_DIR}"
    "${FLUTTER_BIN}" create --platforms="${FLUTTER_PLATFORMS}" .
  )
}

[[ -d "${FLUTTER_DIR}" ]] || die "Flutter app not found: ${FLUTTER_DIR}"
FLUTTER_BIN="$(resolve_flutter)" || die "flutter not found in PATH"

SDK_ROOT="$(resolve_sdk_root)" || die "Android SDK not found; install Android SDK or set ANDROID_HOME"
ADB_BIN="$(resolve_tool "${SDK_ROOT}" adb)" || die "adb not found under ${SDK_ROOT}"
SERIAL="$(resolve_serial)"
API_BASE_URL="$(resolve_api_base_url)"

export ADB_SERIAL="${SERIAL}"
export ANDROID_SERIAL="${SERIAL}"

echo "INFO: flutter target ${SERIAL}" >&2
echo "INFO: API_BASE_URL=${API_BASE_URL}" >&2

ensure_platforms

(
  cd "${FLUTTER_DIR}"
  "${FLUTTER_BIN}" pub get
)

FLUTTER_ARGS=(
  run
  --dart-define="API_BASE_URL=${API_BASE_URL}"
)

if [[ -n "${FLUTTER_DEVICE_ID:-}" ]]; then
  FLUTTER_ARGS+=(--device-id "${FLUTTER_DEVICE_ID}")
else
  FLUTTER_ARGS+=(--device-id "${SERIAL}")
fi

if [[ "${FLUTTER_RELEASE:-0}" == "1" ]]; then
  FLUTTER_ARGS+=(--release)
fi

echo "== flutter ${FLUTTER_ARGS[*]}" >&2
(
  cd "${FLUTTER_DIR}"
  "${FLUTTER_BIN}" "${FLUTTER_ARGS[@]}"
)
