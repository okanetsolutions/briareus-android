# Build and install

1. Install JDK 21 and Android SDK platform 37 with Android Studio or the SDK command-line tools.
2. Set `JAVA_HOME` to JDK 21 and `ANDROID_HOME` to the Android SDK directory.
3. Run `./gradlew check assembleDebug assembleRelease -Pwerror`.
4. Connect an Android 12 or later phone or tablet with USB debugging enabled.
5. Run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

The application ID is `com.okanetsolutions.briareus.android`; it can be installed alongside the Quest client.

On ARM64 Linux, Google's SDK native tools need an ARM64-compatible `aapt2`. Set `android.aapt2FromMavenOverride=/absolute/path/to/aapt2` in your own `~/.gradle/gradle.properties`. Do not commit that machine-specific path.

## Sign a release

Set these environment variables before `./gradlew assembleRelease`:

- `BRIAREUS_KEYSTORE`: absolute path to your signing keystore.
- `BRIAREUS_KEYSTORE_PASSWORD`: keystore password.
- `BRIAREUS_KEY_ALIAS`: signing alias.
- `BRIAREUS_KEY_PASSWORD`: key password.

Pass `-PversionCode=2 -PversionName=0.2.0` to set release versions. Keep the keystore outside the repository. Without these variables the release APK is unsigned. No Play Store publication or production signing is configured.

## Update shared contracts

1. Check out and review the desired `briareus-contracts` revision.
2. Run `python3 scripts/sync-contracts.py --source /path/to/briareus-contracts`.
3. Review the tokens, wireframes and `contracts.lock.json` together.
4. Run `python3 scripts/sync-contracts.py` and the Gradle checks.

The copy script refuses a dirty contracts checkout. CI verifies every pinned file's SHA-256 without needing GitHub credentials.
