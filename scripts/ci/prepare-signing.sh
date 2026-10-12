#!/usr/bin/env bash
set -euo pipefail
# This key proves the release signing path; it must never be used for distribution.
: "${RUNNER_TEMP:?}"
: "${GITHUB_ENV:?}"
keystore="$RUNNER_TEMP/briareus-ci-signing.p12"
keytool -genkeypair -noprompt -keystore "$keystore" -storetype PKCS12 \
  -storepass ci-only-password -keypass ci-only-password -alias ci \
  -keyalg RSA -keysize 2048 -validity 2 -dname 'CN=Briareus CI ONLY'
{
  echo "BRIAREUS_KEYSTORE=$keystore"
  echo 'BRIAREUS_KEYSTORE_PASSWORD=ci-only-password'
  echo 'BRIAREUS_KEY_ALIAS=ci'
  echo 'BRIAREUS_KEY_PASSWORD=ci-only-password'
} >> "$GITHUB_ENV"
