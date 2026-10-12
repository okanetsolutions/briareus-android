#!/usr/bin/env bash
set -euo pipefail
: "${EXPECTED_WIDE:?}"
: "${DEVICE_SIZE:?}"
: "${DEVICE_DENSITY:?}"
: "${EXPECTED_PAGE_SIZE:?}"
test "$(adb shell getconf PAGE_SIZE | tr -d '\r')" = "$EXPECTED_PAGE_SIZE"
adb shell wm size "$DEVICE_SIZE"
adb shell wm density "$DEVICE_DENSITY"
./gradlew connectedDebugAndroidTest -Pwerror --console=plain \
  "-Pandroid.testInstrumentationRunnerArguments.expectedWide=$EXPECTED_WIDE"
python3 scripts/ci/device-summary.py
# The minified release must start too; debug instrumentation does not exercise R8.
app_id=com.okanetsolutions.briareus.android
if adb shell pm list packages | tr -d '\r' | grep -Fxq "package:$app_id"; then
  adb uninstall "$app_id"
fi
adb install app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n "$app_id/.MainActivity" | tee release-start.txt
grep -q 'Status: ok' release-start.txt
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml release-window.xml
python3 - <<'PY'
from xml.etree import ElementTree as E
root = E.parse('release-window.xml')
assert any('Connect to Briareus' in n.get('text', '') for n in root.iter('node')), 'Release did not show the pairing screen'
PY
adb shell pidof "$app_id"
adb logcat -d > release-logcat.txt
if grep -q 'FATAL EXCEPTION' release-logcat.txt; then
  cat release-logcat.txt
  exit 1
fi
