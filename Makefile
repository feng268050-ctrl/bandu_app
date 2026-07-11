SHELL := /bin/bash
.DEFAULT_GOAL := help

ANDROID_DIR := android
FLUTTER_DIR := flutter_app
APP_ID := com.bandu.tiji

.PHONY: help emulator install-app flutter-run install-app-release flutter-build-release install-android install-phone build-debug relaunch sync adb-remote version

# Run a command with `.env` exported (if present). Variables already supplied on
# the make command line win over values loaded from `.env`.
# Usage: $(call WITH_DOTENV,<command>)
define WITH_DOTENV
bash -c 'set -euo pipefail; __ENV_AVD="$${AVD-}"; __ENV_ADB_SERIAL="$${ADB_SERIAL-}"; __ENV_ANDROID_SERIAL="$${ANDROID_SERIAL-}"; __ENV_EMULATOR_API_LEVEL="$${EMULATOR_API_LEVEL-}"; __ENV_EMULATOR_PORT="$${EMULATOR_PORT-}"; __ENV_EMULATOR_GPU="$${EMULATOR_GPU-}"; __ENV_EMULATOR_SCALE="$${EMULATOR_SCALE-}"; __ENV_EMULATOR_NO_AUDIO="$${EMULATOR_NO_AUDIO-}"; __ENV_EMULATOR_RECREATE="$${EMULATOR_RECREATE-}"; __ENV_EMULATOR_REMOTE_ADB="$${EMULATOR_REMOTE_ADB-}"; __ENV_REBUILD_IMAGE="$${REBUILD_IMAGE-}"; __ENV_SYNC_REMOTE_ADB="$${SYNC_REMOTE_ADB-}"; __ENV_REMOTE_ADB_PORT="$${REMOTE_ADB_PORT-}"; __ENV_API_BASE_URL="$${API_BASE_URL-}"; __ENV_MOBILE_API_BASE_URL="$${MOBILE_API_BASE_URL-}"; __ENV_FLUTTER_DEVICE_ID="$${FLUTTER_DEVICE_ID-}"; __ENV_FLUTTER_RELEASE="$${FLUTTER_RELEASE-}"; set -a; [[ -f .env ]] && source .env; set +a; [[ -n "$$__ENV_AVD" ]] && export AVD="$$__ENV_AVD"; [[ -n "$$__ENV_ADB_SERIAL" ]] && export ADB_SERIAL="$$__ENV_ADB_SERIAL"; [[ -n "$$__ENV_ANDROID_SERIAL" ]] && export ANDROID_SERIAL="$$__ENV_ANDROID_SERIAL"; [[ -n "$$__ENV_EMULATOR_API_LEVEL" ]] && export EMULATOR_API_LEVEL="$$__ENV_EMULATOR_API_LEVEL"; [[ -n "$$__ENV_EMULATOR_PORT" ]] && export EMULATOR_PORT="$$__ENV_EMULATOR_PORT"; [[ -n "$$__ENV_EMULATOR_GPU" ]] && export EMULATOR_GPU="$$__ENV_EMULATOR_GPU"; [[ -n "$$__ENV_EMULATOR_SCALE" ]] && export EMULATOR_SCALE="$$__ENV_EMULATOR_SCALE"; [[ -n "$$__ENV_EMULATOR_NO_AUDIO" ]] && export EMULATOR_NO_AUDIO="$$__ENV_EMULATOR_NO_AUDIO"; [[ -n "$$__ENV_EMULATOR_RECREATE" ]] && export EMULATOR_RECREATE="$$__ENV_EMULATOR_RECREATE"; [[ -n "$$__ENV_EMULATOR_REMOTE_ADB" ]] && export EMULATOR_REMOTE_ADB="$$__ENV_EMULATOR_REMOTE_ADB"; [[ -n "$$__ENV_REBUILD_IMAGE" ]] && export REBUILD_IMAGE="$$__ENV_REBUILD_IMAGE"; [[ -n "$$__ENV_SYNC_REMOTE_ADB" ]] && export SYNC_REMOTE_ADB="$$__ENV_SYNC_REMOTE_ADB"; [[ -n "$$__ENV_REMOTE_ADB_PORT" ]] && export REMOTE_ADB_PORT="$$__ENV_REMOTE_ADB_PORT"; [[ -n "$$__ENV_API_BASE_URL" ]] && export API_BASE_URL="$$__ENV_API_BASE_URL"; [[ -n "$$__ENV_MOBILE_API_BASE_URL" ]] && export MOBILE_API_BASE_URL="$$__ENV_MOBILE_API_BASE_URL"; [[ -n "$$__ENV_FLUTTER_DEVICE_ID" ]] && export FLUTTER_DEVICE_ID="$$__ENV_FLUTTER_DEVICE_ID"; [[ -n "$$__ENV_FLUTTER_RELEASE" ]] && export FLUTTER_RELEASE="$$__ENV_FLUTTER_RELEASE"; [[ -z "$${ANDROID_SERIAL:-}" && -n "$${ADB_SERIAL:-}" ]] && export ANDROID_SERIAL="$$ADB_SERIAL"; [[ -z "$${ADB_SERIAL:-}" && -n "$${ANDROID_SERIAL:-}" ]] && export ADB_SERIAL="$$ANDROID_SERIAL"; $(1)'
endef

help:
	@echo "Wrong Notebook - Make targets"
	@echo ""
	@echo "Flutter mobile (primary):"
	@echo "  make install-app           # build and run Flutter app on connected phone"
	@echo "  make flutter-run           # alias of install-app"
	@echo "  make install-app-release   # build release Flutter APK and adb install it"
	@echo "  make flutter-build-release # build release Flutter APK only"
	@echo ""
	@echo "Legacy Android (Compose, migration only):"
	@echo "  make emulator              # start/create AVD, push debug APK as system priv-app, launch $(APP_ID)"
	@echo "  make install-android       # build legacy debug APK and install on connected phone"
	@echo "  make install-phone         # alias of install-android"
	@echo "  make build-debug           # build legacy debug APK only"
	@echo "  make install-debug         # install legacy debug APK on selected adb device/emulator"
	@echo "  make sync                  # incremental legacy build + deploy + relaunch (hot sync)"
	@echo "  make adb-remote            # host-assisted adb tcpip 5555 for emulator/LAN debugging"
	@echo "  make relaunch              # force-stop + launch legacy $(APP_ID) on adb device/emulator"
	@echo "  make version               # print current app version"
	@echo ""
	@echo "Common env vars:"
	@echo "  API_BASE_URL=<url>         # Flutter backend URL, e.g. http://192.168.1.10:3000/api/mobile/v1"
	@echo "  MOBILE_API_BASE_URL=<url>  # alias for API_BASE_URL"
	@echo "  FLUTTER_DEVICE_ID=<id>     # optional flutter device id override"
	@echo "  FLUTTER_RELEASE=1          # run flutter in release mode"
	@echo "  AVD=<name>                 # AVD name; default Bandu_Tiji_Tablet"
	@echo "  EMULATOR_API_LEVEL=<n>     # system image API for auto-created AVD; default 36"
	@echo "  EMULATOR_PORT=<n>          # emulator adb serial port; default 5554"
	@echo "  EMULATOR_GPU=<mode>        # optional emulator -gpu mode"
	@echo "  EMULATOR_SCALE=<f>         # optional emulator window scale, e.g. 0.75"
	@echo "  EMULATOR_NO_AUDIO=1        # add emulator -no-audio"
	@echo "  EMULATOR_RECREATE=1        # delete and recreate the AVD before launch"
	@echo "  REBUILD_IMAGE=1            # alias for EMULATOR_RECREATE=1"
	@echo "  ADB_SERIAL=<serial>        # selected adb target for install-app/install-android/relaunch/sync"
	@echo "  INSTALL_SKIP_BUILD=1       # install existing legacy APK only (skip Gradle build)"
	@echo "  INSTALL_NO_RELAUNCH=1      # skip legacy app relaunch after install-android"
	@echo "  SYNC_SKIP_BUILD=1          # deploy existing legacy APK only (skip Gradle build)"
	@echo "  SYNC_NO_RELAUNCH=1         # skip app relaunch after deploy"
	@echo "  SYNC_FAST_REBOOT=0         # full reboot instead of soft reboot for priv-app sync"
	@echo "  SYNC_PRIVAPP=1             # force system priv-app sync and push privapp permissions"
	@echo "  SYNC_REMOTE_ADB=0          # skip host-assisted adb tcpip for emulator sync"
	@echo "  REMOTE_ADB_PORT=<n>        # adb tcpip port; default 5555"
	@echo ""
	@echo "Examples:"
	@echo "  API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make install-app"
	@echo "  make emulator"
	@echo "  EMULATOR_NO_AUDIO=1 make emulator"
	@echo "  AVD=Pixel_Tablet_API_36 EMULATOR_PORT=5556 make emulator"
	@echo "  make install-android"
	@echo "  INSTALL_SKIP_BUILD=1 make install-android"
	@echo "  make sync"
	@echo "  ADB_SERIAL=emulator-5554 make sync"
	@echo "  ADB_SERIAL=emulator-5556 SYNC_PRIVAPP=1 make sync"
	@echo "  ADB_SERIAL=emulator-5556 make adb-remote"
	@echo "  SYNC_NO_RELAUNCH=1 make sync"

install-app flutter-run:
	@chmod +x scripts/flutter-device.sh
	@$(call WITH_DOTENV,./scripts/flutter-device.sh)

install-app-release:
	@chmod +x scripts/flutter-release-install.sh
	@$(call WITH_DOTENV,./scripts/flutter-release-install.sh)

flutter-build-release:
	@chmod +x scripts/flutter-release-install.sh
	@$(call WITH_DOTENV,FLUTTER_BUILD_ONLY=1 ./scripts/flutter-release-install.sh)

emulator:
	@chmod +x scripts/emulator-launch.sh
	@$(call WITH_DOTENV,./scripts/emulator-launch.sh)

build-debug:
	@cd "$(ANDROID_DIR)" && ./gradlew :app:assembleDebug --parallel --build-cache -x test -x lint

install-android install-phone:
	@chmod +x scripts/device-install.sh
	@$(call WITH_DOTENV,./scripts/device-install.sh)

install-debug:
	@$(call WITH_DOTENV,cd "$(ANDROID_DIR)" && ./gradlew :app:installDebug)

sync:
	@chmod +x scripts/device-sync.sh
	@$(call WITH_DOTENV,./scripts/device-sync.sh)

adb-remote:
	@$(call WITH_DOTENV,port="$${REMOTE_ADB_PORT:-5555}"; serial="$${ADB_SERIAL:-$${ANDROID_SERIAL:-}}"; ADB_CMD=(adb); [[ -n "$$serial" ]] && ADB_CMD+=(-s "$$serial"); "$${ADB_CMD[@]}" tcpip "$$port")

relaunch:
	@$(call WITH_DOTENV,adb $${ADB_SERIAL:+-s "$$ADB_SERIAL"} shell am force-stop "$(APP_ID)" >/dev/null || true; adb $${ADB_SERIAL:+-s "$$ADB_SERIAL"} shell monkey -p "$(APP_ID)" -c android.intent.category.LAUNCHER 1 >/dev/null)

version:
	@sed -n '1p' VERSION
