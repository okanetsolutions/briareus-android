# Verification

Initial Android port, checked on 2026-10-12.

| Check | Result |
| --- | --- |
| `./gradlew check assembleDebug assembleRelease -Pwerror` | Passed with JDK 21 and Android SDK 37 on ARM64 Linux |
| Core JVM tests | 77 passed, zero failures |
| Android lint | No issues found; warnings treated as errors |
| Detekt | Passed for core and app |
| `python3 scripts/sync-contracts.py` | Verified all 55 pinned token, resource and wireframe files |
| APK manifest inspection | Application ID `com.okanetsolutions.briareus.android`, minimum API 31, target API 37 |
| Source review | Fixed width-change draft loss, uncertain send/start retries, stale connection responses and unsuccessful revocation behavior |

The local ARM64 build uses an existing ARM64 `aapt2` override; CI builds with Google's Linux tools. The debug APK is development-signed. The release APK is unsigned without a supplied signing key.

No physical-device, emulator, live-server, microphone, notification-delivery or foldable acceptance session was performed. No separate QA worker ran. Before release, validate pairing, sending, rotation, keyboard insets, phone/tablet layouts and permission revocation against a test server. Do not treat the automated results as evidence of live integration or full Swift parity.
