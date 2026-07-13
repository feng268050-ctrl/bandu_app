# bandu_app

`bandu_app` is the Flutter mobile client for Bandu. The Web UI, Mobile API,
Prisma database, AI analysis pipeline, and deployment files now live in the
separate `bandu_web` repository.

## Scope

- Flutter Material 3 app shell.
- Bottom navigation: 首页、错题本、拍题、练习、我的。
- Login, registration, logout, token restore, and refresh handling.
- Capture, AI analysis, confirm, save, list, detail, edit, delete, and retry flows.
- Practice generation and practice record submission.
- Profile page, local avatar settings, local cache, and Android device name bridge.
- Android-first build and install workflow.

## Backend

The app talks to:

```text
http://<server-ip>:3000/api/mobile/v1
```

For Android Emulator on the same host, use:

```text
http://10.0.2.2:3000/api/mobile/v1
```

## Run

```bash
flutter pub get
API_BASE_URL=http://10.0.2.2:3000/api/mobile/v1 make run
```

## Build And Install

```bash
make apk
ADB_SERIAL=emulator-5556 make install
```

`ADB_SERIAL` defaults to `emulator-5556` for local validation.
