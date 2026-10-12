# Working on Briareus for Android

Native Kotlin and Jetpack Compose for phones and tablets. `core/` is Android-free; `app/` owns Android storage, lifecycle and UI. Read `docs/features.md` before claiming parity with another client.

- Run `./gradlew check lintRelease assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease -Pwerror` and `python3 scripts/sync-contracts.py`.
- Keep server routes in `core/Routes.kt`; compare changes against the server API and pinned Swift reference.
- Enforce server capability and token permissions before mutations, including notification and voice actions.
- Require HTTPS, reject redirects, and never automatically retry writes.
- Keep credentials and cached private data encrypted and out of backups.
- Keep navigation and drafts across rotation and phone/tablet width changes.
- Consume generated contracts tokens; update through `scripts/sync-contracts.py`, not hand-edited palettes.
- Add behavior tests for parsers and state transitions; do not claim device verification from a compile result.
- Work on a feature branch; do not push or merge into shared branches without authorization.
