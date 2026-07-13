SHELL := /bin/bash
.DEFAULT_GOAL := help

APP_ID := com.bandu.tiji
ADB_SERIAL ?= emulator-5556
API_BASE_URL ?= http://10.0.2.2:3000/api/mobile/v1
APP_VERSION ?= v0.1.1

.PHONY: help deps analyze test run apk install relaunch clean version

help:
	@echo "bandu_app Flutter targets"
	@echo "  make deps      # flutter pub get"
	@echo "  make analyze   # flutter analyze"
	@echo "  make test      # flutter test"
	@echo "  make run       # flutter run with API_BASE_URL"
	@echo "  make apk       # build release APK"
	@echo "  make install   # install release APK to ADB_SERIAL=$(ADB_SERIAL)"
	@echo "  make relaunch  # relaunch $(APP_ID)"
	@echo "  make version   # print VERSION"

deps:
	flutter pub get

analyze:
	flutter analyze

test:
	flutter test

run:
	flutter run --dart-define=API_BASE_URL=$(API_BASE_URL) --dart-define=APP_VERSION=$(APP_VERSION)

apk:
	flutter build apk --release --dart-define=API_BASE_URL=$(API_BASE_URL) --dart-define=APP_VERSION=$(APP_VERSION)

install: apk
	adb -s $(ADB_SERIAL) install -r build/app/outputs/flutter-apk/app-release.apk
	$(MAKE) relaunch

relaunch:
	adb -s $(ADB_SERIAL) shell am force-stop $(APP_ID) >/dev/null || true
	adb -s $(ADB_SERIAL) shell monkey -p $(APP_ID) -c android.intent.category.LAUNCHER 1 >/dev/null

clean:
	flutter clean

version:
	@sed -n '1p' VERSION
