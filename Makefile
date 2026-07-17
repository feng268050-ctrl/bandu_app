SHELL := /bin/bash
.DEFAULT_GOAL := help

APP_ID := com.bandu.tiji
ADB_SERIAL ?= emulator-5556
API_BASE_URL ?=
EMULATOR_API_BASE_URL ?= http://10.0.2.2:3000/api/mobile/v1
DEVICE_API_BASE_URL ?= http://100.69.41.14:3000/api/mobile/v1
PROD_API_BASE_URL := https://aibandu.dpdns.org/api/mobile/v1
RELEASE_API_BASE_URL ?= $(PROD_API_BASE_URL)
APP_VERSION ?= $(shell tr -d ' \n\r' < VERSION 2>/dev/null || echo v0.2.0)
BYPASS_AUTH ?= false

ADB_SERIAL_OVERRIDE := $(if $(filter environment command line override,$(origin ADB_SERIAL)),$(ADB_SERIAL),)
API_BASE_URL_OVERRIDE := $(if $(filter environment command line override,$(origin API_BASE_URL)),$(API_BASE_URL),)
EMULATOR_API_BASE_URL_OVERRIDE := $(if $(filter environment command line override,$(origin EMULATOR_API_BASE_URL)),$(EMULATOR_API_BASE_URL),)
DEVICE_API_BASE_URL_OVERRIDE := $(if $(filter environment command line override,$(origin DEVICE_API_BASE_URL)),$(DEVICE_API_BASE_URL),)
RELEASE_API_BASE_URL_OVERRIDE := $(if $(filter environment command line override,$(origin RELEASE_API_BASE_URL)),$(RELEASE_API_BASE_URL),)

define with_dotenv
@set -euo pipefail; \
	adb_serial_override="$(ADB_SERIAL_OVERRIDE)"; \
	api_base_url_override="$(API_BASE_URL_OVERRIDE)"; \
	emulator_api_base_url_override="$(EMULATOR_API_BASE_URL_OVERRIDE)"; \
	device_api_base_url_override="$(DEVICE_API_BASE_URL_OVERRIDE)"; \
	release_api_base_url_override="$(RELEASE_API_BASE_URL_OVERRIDE)"; \
	export APP_ID="$(APP_ID)" ADB_SERIAL="$(ADB_SERIAL)"; \
	export EMULATOR_API_BASE_URL="$(EMULATOR_API_BASE_URL)" DEVICE_API_BASE_URL="$(DEVICE_API_BASE_URL)" RELEASE_API_BASE_URL="$(RELEASE_API_BASE_URL)"; \
	export APP_VERSION="$(APP_VERSION)" BYPASS_AUTH="$(BYPASS_AUTH)"; \
	[ -f .env ] && set -a && source .env && set +a; \
	[ -n "$$adb_serial_override" ] && export ADB_SERIAL="$$adb_serial_override"; \
	[ -n "$$api_base_url_override" ] && export API_BASE_URL="$$api_base_url_override"; \
	[ -n "$$emulator_api_base_url_override" ] && export EMULATOR_API_BASE_URL="$$emulator_api_base_url_override"; \
	[ -n "$$device_api_base_url_override" ] && export DEVICE_API_BASE_URL="$$device_api_base_url_override"; \
	[ -n "$$release_api_base_url_override" ] && export RELEASE_API_BASE_URL="$$release_api_base_url_override"; \
	$(1)
endef

.PHONY: help deps analyze test verify verify-prod-config run run-prod emulator sync apk apk-prod install install-debug relaunch clean version

help:
	@echo "bandu_app Flutter targets"
	@echo "  make deps          # flutter pub get"
	@echo "  make analyze       # flutter analyze"
	@echo "  make test          # flutter test"
	@echo "  make verify        # analyze + test + debug APK"
	@echo "  make run           # flutter run with API_BASE_URL"
	@echo "  make run-prod      # flutter run with the public production API"
	@echo "  make emulator      # start emulator ($(ADB_SERIAL)) and flutter run"
	@echo "  make sync          # debug build + install + relaunch on $(ADB_SERIAL)"
	@echo "  make apk-prod      # build release APK with the public production API"
	@echo "  make verify-prod-config # validate production endpoint policy"
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

verify: analyze test
	@chmod +x scripts/flutter-build-apk.sh
	$(call with_dotenv,./scripts/flutter-build-apk.sh debug)

verify-prod-config:
	@chmod +x scripts/verify-production-config.sh
	@PROD_API_BASE_URL="$(PROD_API_BASE_URL)" BYPASS_AUTH=false ./scripts/verify-production-config.sh

run:
	$(call with_dotenv,source scripts/api-base-url.sh; resolved_api="$$(resolve_api_base_url emulator)"; echo "INFO: emulator API_BASE_URL=$$resolved_api" >&2; flutter run --dart-define=API_BASE_URL="$$resolved_api" --dart-define=APP_VERSION="$$APP_VERSION" --dart-define=BYPASS_AUTH="$$BYPASS_AUTH")

run-prod: verify-prod-config
	@API_BASE_URL="$(PROD_API_BASE_URL)" RELEASE_API_BASE_URL="$(PROD_API_BASE_URL)" BYPASS_AUTH=false APP_ENV=production flutter run --dart-define=APP_ENV=production --dart-define=API_BASE_URL="$(PROD_API_BASE_URL)" --dart-define=APP_VERSION="$(APP_VERSION)" --dart-define=BYPASS_AUTH=false

emulator:
	@chmod +x scripts/emulator-launch.sh
	$(call with_dotenv,./scripts/emulator-launch.sh)
	$(call with_dotenv,source scripts/api-base-url.sh; resolved_api="$$(resolve_api_base_url emulator)"; echo "INFO: emulator API_BASE_URL=$$resolved_api" >&2; flutter run -d "$$ADB_SERIAL" --dart-define=API_BASE_URL="$$resolved_api" --dart-define=APP_VERSION="$$APP_VERSION" --dart-define=BYPASS_AUTH="$$BYPASS_AUTH")

sync:
	@chmod +x scripts/flutter-sync.sh
	$(call with_dotenv,./scripts/flutter-sync.sh)

apk: apk-prod

apk-prod: verify-prod-config
	@chmod +x scripts/flutter-build-apk.sh
	@API_BASE_URL="$(PROD_API_BASE_URL)" RELEASE_API_BASE_URL="$(PROD_API_BASE_URL)" BYPASS_AUTH=false APP_ENV=production ./scripts/flutter-build-apk.sh release

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
