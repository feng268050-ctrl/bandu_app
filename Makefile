SHELL := /bin/bash
.DEFAULT_GOAL := help

ANDROID_DIR := android
APP_ID := com.bandu.tiji

.PHONY: help emulator install-debug relaunch sync

# Run a command with `.env` exported (if present). Variables already supplied on
# the make command line win over values loaded from `.env`.
# Usage: $(call WITH_DOTENV,<command>)
define WITH_DOTENV
bash -c 'set -euo pipefail; __ENV_AVD="$${AVD-}"; __ENV_ADB_SERIAL="$${ADB_SERIAL-}"; __ENV_ANDROID_SERIAL="$${ANDROID_SERIAL-}"; __ENV_EMULATOR_API_LEVEL="$${EMULATOR_API_LEVEL-}"; __ENV_EMULATOR_PORT="$${EMULATOR_PORT-}"; __ENV_EMULATOR_GPU="$${EMULATOR_GPU-}"; __ENV_EMULATOR_SCALE="$${EMULATOR_SCALE-}"; __ENV_EMULATOR_NO_AUDIO="$${EMULATOR_NO_AUDIO-}"; __ENV_EMULATOR_RECREATE="$${EMULATOR_RECREATE-}"; __ENV_REBUILD_IMAGE="$${REBUILD_IMAGE-}"; set -a; [[ -f .env ]] && source .env; set +a; [[ -n "$$__ENV_AVD" ]] && export AVD="$$__ENV_AVD"; [[ -n "$$__ENV_ADB_SERIAL" ]] && export ADB_SERIAL="$$__ENV_ADB_SERIAL"; [[ -n "$$__ENV_ANDROID_SERIAL" ]] && export ANDROID_SERIAL="$$__ENV_ANDROID_SERIAL"; [[ -n "$$__ENV_EMULATOR_API_LEVEL" ]] && export EMULATOR_API_LEVEL="$$__ENV_EMULATOR_API_LEVEL"; [[ -n "$$__ENV_EMULATOR_PORT" ]] && export EMULATOR_PORT="$$__ENV_EMULATOR_PORT"; [[ -n "$$__ENV_EMULATOR_GPU" ]] && export EMULATOR_GPU="$$__ENV_EMULATOR_GPU"; [[ -n "$$__ENV_EMULATOR_SCALE" ]] && export EMULATOR_SCALE="$$__ENV_EMULATOR_SCALE"; [[ -n "$$__ENV_EMULATOR_NO_AUDIO" ]] && export EMULATOR_NO_AUDIO="$$__ENV_EMULATOR_NO_AUDIO"; [[ -n "$$__ENV_EMULATOR_RECREATE" ]] && export EMULATOR_RECREATE="$$__ENV_EMULATOR_RECREATE"; [[ -n "$$__ENV_REBUILD_IMAGE" ]] && export REBUILD_IMAGE="$$__ENV_REBUILD_IMAGE"; [[ -z "$${ANDROID_SERIAL:-}" && -n "$${ADB_SERIAL:-}" ]] && export ANDROID_SERIAL="$$ADB_SERIAL"; [[ -z "$${ADB_SERIAL:-}" && -n "$${ANDROID_SERIAL:-}" ]] && export ADB_SERIAL="$$ANDROID_SERIAL"; $(1)'
endef

help:
	@echo "Wrong Notebook - Make targets"
	@echo ""
	@echo "Android emulator:"
	@echo "  make emulator              # start/create AVD, push debug APK as system priv-app, launch $(APP_ID)"
	@echo "  make install-debug         # install debug APK on selected adb device/emulator"
	@echo "  make sync                  # incremental build + deploy + relaunch (hot sync)"
	@echo "  make relaunch              # force-stop + launch $(APP_ID) on selected adb device/emulator"
	@echo ""
	@echo "Common env vars:"
	@echo "  AVD=<name>                 # AVD name; default Bandu_Tiji_Tablet"
	@echo "  EMULATOR_API_LEVEL=<n>     # system image API for auto-created AVD; default 36"
	@echo "  EMULATOR_PORT=<n>          # emulator adb serial port; default 5554"
	@echo "  EMULATOR_GPU=<mode>        # optional emulator -gpu mode"
	@echo "  EMULATOR_SCALE=<f>         # optional emulator window scale, e.g. 0.75"
	@echo "  EMULATOR_NO_AUDIO=1        # add emulator -no-audio"
	@echo "  EMULATOR_RECREATE=1        # delete and recreate the AVD before launch"
	@echo "  REBUILD_IMAGE=1            # alias for EMULATOR_RECREATE=1"
	@echo "  ADB_SERIAL=<serial>        # selected adb target for install-debug/relaunch/sync"
	@echo "  SYNC_SKIP_BUILD=1          # deploy existing APK only (skip Gradle build)"
	@echo "  SYNC_NO_RELAUNCH=1         # skip app relaunch after deploy"
	@echo "  SYNC_FAST_REBOOT=0         # full reboot instead of soft reboot for priv-app sync"
	@echo ""
	@echo "Examples:"
	@echo "  make emulator"
	@echo "  EMULATOR_NO_AUDIO=1 make emulator"
	@echo "  AVD=Pixel_Tablet_API_36 EMULATOR_PORT=5556 make emulator"
	@echo "  make sync"
	@echo "  ADB_SERIAL=emulator-5554 make sync"
	@echo "  SYNC_NO_RELAUNCH=1 make sync"

emulator:
	@chmod +x scripts/emulator-launch.sh
	@$(call WITH_DOTENV,./scripts/emulator-launch.sh)

install-debug:
	@$(call WITH_DOTENV,cd "$(ANDROID_DIR)" && ./gradlew :app:installDebug)

sync:
	@chmod +x scripts/device-sync.sh
	@$(call WITH_DOTENV,./scripts/device-sync.sh)

relaunch:
	@$(call WITH_DOTENV,adb $${ADB_SERIAL:+-s "$$ADB_SERIAL"} shell am force-stop "$(APP_ID)" >/dev/null || true; adb $${ADB_SERIAL:+-s "$$ADB_SERIAL"} shell monkey -p "$(APP_ID)" -c android.intent.category.LAUNCHER 1 >/dev/null)
