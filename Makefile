SHELL := /bin/bash
.DEFAULT_GOAL := help

APP_ID := com.bandu.tiji
ADB_SERIAL ?= emulator-5556
API_BASE_URL ?= http://10.0.2.2:3000/api/mobile/v1
APP_VERSION ?= $(shell tr -d ' \n\r' < VERSION 2>/dev/null || echo v0.1.1)

FLUTTER_DART_DEFINES := --dart-define=API_BASE_URL=$(API_BASE_URL) --dart-define=APP_VERSION=$(APP_VERSION)

define with_dotenv
@set -euo pipefail; \
	export APP_ID="$(APP_ID)" ADB_SERIAL="$(ADB_SERIAL)" API_BASE_URL="$(API_BASE_URL)" APP_VERSION="$(APP_VERSION)"; \
	[ -f .env ] && set -a && source .env && set +a; \
	$(1)
endef

.PHONY: help deps analyze test run emulator sync apk install install-debug relaunch clean version

help:
	@echo "bandu_app Flutter targets"
	@echo "  make deps          # flutter pub get"
	@echo "  make analyze       # flutter analyze"
	@echo "  make test          # flutter test"
	@echo "  make run           # flutter run with API_BASE_URL"
	@echo "  make emulator      # start emulator ($(ADB_SERIAL)) and flutter run"
	@echo "  make sync          # debug build + install + relaunch on $(ADB_SERIAL)"
	@echo "  make apk           # build release APK"
	@echo "  make install       # install release APK to ADB_SERIAL=$(ADB_SERIAL)"
	@echo "  make install-debug # install existing debug APK"
	@echo "  make relaunch      # relaunch $(APP_ID)"
	@echo "  make version       # print VERSION"

deps:
	flutter pub get

analyze:
	flutter analyze

test:
	flutter test

run:
	flutter run $(FLUTTER_DART_DEFINES)

emulator:
	@chmod +x scripts/emulator-launch.sh
	@$(call with_dotenv,./scripts/emulator-launch.sh)
	@$(call with_dotenv,flutter run -d "$$ADB_SERIAL" --dart-define=API_BASE_URL="$$API_BASE_URL" --dart-define=APP_VERSION="$$APP_VERSION")

sync:
	@chmod +x scripts/flutter-sync.sh
	@$(call with_dotenv,./scripts/flutter-sync.sh)

apk:
	flutter build apk --release $(FLUTTER_DART_DEFINES)

install: apk
	adb -s $(ADB_SERIAL) install -r build/app/outputs/flutter-apk/app-release.apk
	$(MAKE) relaunch

install-debug:
	adb -s $(ADB_SERIAL) install -r build/app/outputs/flutter-apk/app-debug.apk
	$(MAKE) relaunch

relaunch:
	adb -s $(ADB_SERIAL) shell am force-stop $(APP_ID) >/dev/null 2>&1 || true
	adb -s $(ADB_SERIAL) shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $(APP_ID)/.MainActivity >/dev/null 2>&1 || \
	adb -s $(ADB_SERIAL) shell monkey -p $(APP_ID) -c android.intent.category.LAUNCHER 1 >/dev/null

clean:
	flutter clean

version:
	@sed -n '1p' VERSION
