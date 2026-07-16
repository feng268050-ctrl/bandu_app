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
- Offline-marked error/statistics cache, persistent pending uploads, retry, and cleanup.
- Profile page, network diagnostics, local avatar settings, and Android device name bridge.
- Android-first build and install workflow.

## Architecture

The Flutter client follows the five-layer design documented in
[docs/bandu_flutter_five_layer_architecture.md](docs/bandu_flutter_five_layer_architecture.md):

```text
lib/application  pages, state, use cases, domain models, repository contracts
lib/build        bootstrap and framework dependency composition
lib/components   reusable UI and design system
lib/framework    network, persistence, camera, device, and repository implementations
lib/conversion   API DTOs, mappers, and persistence records
lib/main.dart    minimal process entry
```

Framework implementations are injected by `lib/build`. Architecture tests
prevent application code from importing platform plugins or concrete
repositories, and confine raw JSON maps to the conversion adapter boundary.

Material 3 component conventions are documented in
[docs/bandu_material3_component_guideline.md](docs/bandu_material3_component_guideline.md).
Shared visual behavior lives in `lib/components`; feature-specific cards,
selectors, and summaries remain under each feature's `presentation/widgets`.

## Backend

Production uses one public domain and does not depend on Tailscale, a LAN, or a
developer machine:

```text
https://aibandu.dpdns.org/api/mobile/v1
```

Health checks use:

```text
https://aibandu.dpdns.org/api/health
```

For local Android Emulator development only, use:

```text
http://10.0.2.2:3000/api/mobile/v1
```

## Run

```bash
flutter pub get
API_BASE_URL=http://10.0.2.2:3000/api/mobile/v1 make run
```

本地开发推荐：

```bash
cp .env.example .env   # optional
make emulator          # start emulator + flutter run
make sync              # after code changes: debug build + install + relaunch
make verify            # analyze + test + debug APK
```

See [docs/development.md](docs/development.md) for emulator setup, device targets, and troubleshooting.

## Build And Install

```bash
make verify-prod-config
make apk-prod
ADB_SERIAL=emulator-5556 make install
```

Release builds require the fixed team keystore configured through
`android/key.properties`; see [docs/development.md](docs/development.md).

`ADB_SERIAL` defaults to `emulator-5556` for local validation.
