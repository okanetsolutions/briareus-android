# Briareus for Android

A native Kotlin and Jetpack Compose client for [Briareus](https://github.com/nadinyamaui/briareus), for Android phones and tablets running Android 12 or later.

The app uses the mobile navigation of [briareus-swift](https://github.com/okanetsolutions/briareus-swift), the tested Kotlin client from [briareus-quest](https://github.com/okanetsolutions/briareus-quest), and the shared [design contracts and wireframes](https://github.com/okanetsolutions/briareus-contracts). The app screens are native Compose. Run previews use an embedded Android WebView.

This is an initial Android port, not complete Swift or Windows feature parity. See [feature coverage](docs/features.md) for implemented behavior and remaining work.

## Use the app

1. Open your server's **Settings → Devices and clients** and create a device token for the projects you need.
2. Install the debug APK from the CI artifact or [build it locally](docs/build.md).
3. Enter your server's HTTPS address and the device token.
4. Open a project or start a conversation with a Manage token.

Projects, Findings, Usage and Settings are available through the bottom navigation. At 1024dp and wider, the project list stays beside the selected conversation or project page. Narrower windows use a single pane with Android Back navigation. Layout depends on the current window width, so tablet split-screen behaves like a narrow phone when necessary.

Compose follows the system's light or dark theme. Text fields accept the Android keyboard, with optional voice notes. Agent replies render Markdown, selectable code and horizontally scrolling tables with **Copy table**.

## Build and check

Install JDK 21 and Android SDK platform 37, then run:

```sh
./gradlew check assembleDebug assembleRelease -Pwerror
python3 scripts/sync-contracts.py
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Release builds are unsigned unless you supply your own signing configuration. See [build instructions](docs/build.md).

CI also runs phone/tablet emulator tests, coverage gates, dependency review, CodeQL and signed release-package verification. Version tags on `main` run those checks before preparing a production-signed draft release. See [CI and release delivery](docs/ci.md) for the required signing secrets and checks. Physical-device and live-server acceptance remains required before distribution.

## Security

Pairing requires HTTPS and a scoped device token. Credentials and cached responses are encrypted with Android Keystore keys and excluded from backups. The API client rejects redirects, uses system certificate trust, and never automatically retries a write. Server capabilities and token permissions control available actions.

Background alerts are off by default. When enabled, a foreground notification accompanies the event stream; Android can stop it when its background-service allowance expires. This is not a push-notification service.

Voice notes go to your Briareus server. The separate, optional voice conversation sends audio to OpenAI using a key you enter in its settings. It starts only on request. The app includes no analytics SDK.

## Source references

[contracts.lock.json](contracts.lock.json) pins the source revisions. [Architecture](docs/architecture.md) records the reuse decision. [Wireframes](docs/wireframes/README.md) are copied from the pinned contracts revision, and their presence does not mean every screen has been implemented.

The original Kotlin source and shared contracts are MIT licensed; attribution is retained in [LICENSE](LICENSE) and [docs/wireframes/LICENSE](docs/wireframes/LICENSE).
