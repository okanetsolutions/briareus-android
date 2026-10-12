# Android CI and release delivery

Every pull request and push to `main` runs `Android CI`. `Android checks` passes only after every required job succeeds; cancelled, failed or unexpectedly skipped jobs fail that final check. PR builds have no production signing secrets.

| Job | What it verifies |
| --- | --- |
| Build, test, lint and release signing | Gradle wrapper checksum, pinned contracts, all JVM tests, JaCoCo coverage, detekt, debug/release Android lint, debug/test/release APKs and release AAB |
| Release package verification | Disposable-key APK and AAB signatures, application ID, SDK levels, non-debuggable release and 16 KB ZIP alignment |
| Device tests: phone API 31 | Native pairing validation, activity recreation, keyboard input, compact navigation and minified release startup |
| Device tests: tablet API 37 | The same behaviors in the wide layout on a 16 KB system image |
| Dependency vulnerability review | Resolved Gradle dependencies submitted to GitHub, blocking newly introduced high/critical advisories on PRs |
| Security analysis | CodeQL Java/Kotlin security-and-quality queries |
| Formatting | `.editorconfig` |
| Workflow files | actionlint |

Core coverage must remain at least 90% of lines and 65% of branches. These floors start just below the existing suite's measured coverage; they apply to all core classes, without excluding difficult classes to raise the result. Android instrumented tests are separate and are not counted in the JVM coverage report.

The phone runs API 31, the oldest supported Android version. The tablet runs API 37, the app's target SDK, with three-button system navigation: the emulator image's gesture-region sampler crashes SurfaceFlinger before instrumentation starts. Phone tests retain their default navigation mode; gesture navigation on a physical Android 17 device still needs acceptance testing. The emulator uses the supported `software` graphics backend, as described in [Android's acceleration documentation](https://developer.android.com/studio/run/emulator-acceleration). The jobs set window sizes explicitly and assert that they actually exercise the intended compact/wide layout. The job invokes AndroidJUnitRunner directly and requires four completed passing tests plus a successful JUnit summary; an adb exit code alone cannot make an empty run pass. Raw instrumentation output and logcat are retained on failure. After instrumentation, each emulator installs and opens the minified release APK, catching startup problems that a debug-only test would miss.

The workflow uploads APKs, the AAB, R8 mapping, package hashes, test/lint/coverage reports and emulator failure evidence. `Android-packages-ci-only` uses a disposable key and is for testing only; it is not an upgrade channel. Dependabot checks Gradle dependencies and GitHub Actions weekly. CodeQL also runs weekly.

Dependency submission needs `contents: write` on the build job. Fork PR tokens are read-only, so a fork may require a maintainer-owned branch before the complete dependency-submission gate can run. The workflow does not use `pull_request_target` to execute untrusted code with elevated permissions.

## Release delivery

A `vMAJOR.MINOR.PATCH` tag on a commit reachable from `main` starts `Release`. It first runs the same full CI workflow, including device tests, then builds the production-signed APK/AAB and prepares a **draft** GitHub release with both packages, R8 mapping and SHA-256 hashes. It does not publish to Google Play.

Configure these repository secrets before creating a release tag:

- `BRIAREUS_KEYSTORE_BASE64`: base64-encoded production signing keystore.
- `BRIAREUS_KEYSTORE_PASSWORD`: keystore password.
- `BRIAREUS_KEY_ALIAS`: signing alias.
- `BRIAREUS_KEY_PASSWORD`: signing key password.

A release fails if any signing secret is missing. It never falls back to the disposable CI key or an unsigned package. The workflow removes its temporary keystore even if a later step fails. Re-running the same tag replaces assets only while its release is still a draft.

The version name comes from the tag. The version code is the commit count at that tag, so release commits must advance monotonically along `main`. Keep the production signing key consistent for installable upgrades.

## Run the checks locally

```sh
./gradlew check lintRelease assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease -Pwerror
python3 scripts/sync-contracts.py
```

With a connected emulator, `scripts/ci/device-tests.sh` builds and installs the test APK, runs AndroidJUnitRunner directly, and verifies its output; the workflow supplies the expected window size and memory page size. The scripted CI signing and package checks are in `scripts/ci/`; the disposable signing key is generated under the runner's temporary directory.
