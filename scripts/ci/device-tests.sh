#!/usr/bin/env bash
set -euo pipefail
: "${EXPECTED_WIDE:?}"
: "${DEVICE_SIZE:?}"
: "${DEVICE_DENSITY:?}"
: "${EXPECTED_PAGE_SIZE:?}"
trap 'adb logcat -d > device-logcat.txt; adb shell df -h /data > device-storage.txt' EXIT
test "$(adb shell getconf PAGE_SIZE | tr -d '\r')" = "$EXPECTED_PAGE_SIZE"
adb logcat -c
adb shell wm size "$DEVICE_SIZE"
adb shell wm density "$DEVICE_DENSITY"
./gradlew assembleDebug assembleDebugAndroidTest -Pwerror --console=plain
app_id=com.okanetsolutions.briareus.android
adb install -t app/build/outputs/apk/debug/app-debug.apk
adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
device_user=$(adb shell am get-current-user | tr -d '\r')
user_state=''
for attempt in $(seq 1 30); do
  adb shell input keyevent KEYCODE_WAKEUP
  adb shell wm dismiss-keyguard
  adb shell input keyevent KEYCODE_MENU
  user_state=$(adb shell am get-started-user-state "$device_user" | tr -d '\r')
  if [ "$user_state" = RUNNING_UNLOCKED ]; then break; fi
  echo "Waiting for user $device_user: $user_state (attempt $attempt)"
  sleep 2
done
test "$user_state" = RUNNING_UNLOCKED
adb shell am instrument --user "$device_user" -w -r -e expectedWide "$EXPECTED_WIDE" \
  "$app_id.test/androidx.test.runner.AndroidJUnitRunner" | tee device-instrumentation.txt
python3 scripts/ci/device-summary.py
# The minified release must start too; debug instrumentation does not exercise R8.
adb uninstall "$app_id.test"
adb uninstall "$app_id"
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
