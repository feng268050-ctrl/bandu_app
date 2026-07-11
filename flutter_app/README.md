# Bandu Flutter App

This directory is the first Flutter refactor slice described in
`../docs/Architecture optimization.md`.

The existing Next.js application remains the server, admin, export, AI-key, and
database owner. The Flutter client is the daily-use mobile surface.

## Scope

- Material 3 app shell.
- `go_router` bottom navigation: 首页、错题本、拍题、练习、我的。
- Riverpod dependency and state boundaries.
- Dio API client targeting `/api/mobile/v1`.
- Secure refresh-token storage and in-memory access token.
- Capture state machine: `idle -> capturing -> preview -> analyzing -> success/failed`.
- File-backed local cache for error summaries/details, ready to be replaced by a Drift adapter.
- Capture-to-library loop: analyze image, confirm, save to `/error-items`, then update local cache.
- Practice loop: generate a similar question from the latest error item and record the result.

## Run

Install Flutter first, then from this directory:

```bash
flutter create --platforms=android,ios .
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:3000/api/mobile/v1
```

Or from the repo root:

```bash
API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make install-app
```

For a physical device, replace `API_BASE_URL` with the reachable server URL.

Build or install a release APK:

```bash
API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make flutter-build-release
ADB_SERIAL=<serial> API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make install-app-release
```

See `../docs/flutter-migration-status.md` for the current migration checklist.
