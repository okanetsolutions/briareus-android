#!/usr/bin/env bash
set -euo pipefail
apk=app/build/outputs/apk/release/app-release.apk
bundle=app/build/outputs/bundle/release/app-release.aab
tools="$ANDROID_HOME/build-tools/37.0.0"
analyzer="$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer"
test -s "$apk"
test -s "$bundle"
"$tools/apksigner" verify --verbose --print-certs "$apk"
"$tools/zipalign" -c -P 16 -v 4 "$apk"
python3 scripts/ci/check-native-alignment.py "$apk"
LC_ALL=C jarsigner -verify "$bundle" | tee "$RUNNER_TEMP/bundle-signature.txt"
grep -q 'jar verified.' "$RUNNER_TEMP/bundle-signature.txt"
test "$("$analyzer" manifest application-id "$apk")" = 'com.okanetsolutions.briareus.android'
test "$("$analyzer" manifest min-sdk "$apk")" = '31'
test "$("$analyzer" manifest target-sdk "$apk")" = '37'
test "$("$analyzer" manifest debuggable "$apk")" = 'false'
mkdir -p app/build/release-checks
{
  (cd app/build/outputs/apk/release && sha256sum app-release.apk)
  (cd app/build/outputs/bundle/release && sha256sum app-release.aab)
  (cd app/build/outputs/mapping/release && sha256sum mapping.txt)
} > app/build/release-checks/SHA256SUMS
