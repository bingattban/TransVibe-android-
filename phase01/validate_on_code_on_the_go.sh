#!/bin/sh
# Phase 01 — on-device build evidence collector. No installation or data changes.
# Usage: sh phase01/validate_on_code_on_the_go.sh [log-directory]
set -u

HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 2
ROOT=$(CDPATH= cd -- "$HERE/.." && pwd -P) || exit 2
LOGS=${1:-"$HERE/cotg_device_logs"}
mkdir -p "$LOGS" || exit 2
LOGS=$(CDPATH= cd -- "$LOGS" && pwd -P) || exit 2
SUMMARY="$LOGS/PHASE01_BUILD_RESULTS.txt"
: > "$SUMMARY" || exit 2

record() { printf '%s\n' "$*" | tee -a "$SUMMARY"; }
record 'PHASE 01 — Code On The Go build gate'
record 'IMPORTANT: PASS below covers build tasks ONLY, NOT ARM64 install/launch or native speech recognition.'
record 'No uninstall / signing / existing app data modification is performed by this script.'
record "ROOT: $ROOT"
record "EVIDENCE: $LOGS"
(record "DATE_UTC: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"; uname -a; java -version 2>&1) > "$LOGS/HOST_AND_JAVA.txt" 2>&1

if ! command -v java >/dev/null 2>&1; then
    record 'INCOMPLETE: Java is not available; configure JDK 17 or a compatible newer JDK.'
    exit 1
fi
if ! test -s "$ROOT/gradle/wrapper/gradle-wrapper.jar"; then
    record 'INCOMPLETE: gradle-wrapper.jar missing.'
    exit 1
fi
if ! test -s "$ROOT/gradlew"; then
    record 'INCOMPLETE: gradlew launcher missing.'
    exit 1
fi

# Inspect only the exact checksums of package files; do not inspect or print secrets.
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$ROOT/gradle/wrapper/gradle-wrapper.jar" > "$LOGS/WRAPPER_SHA256.txt"
elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$ROOT/gradle/wrapper/gradle-wrapper.jar" > "$LOGS/WRAPPER_SHA256.txt"
else
    record 'WARNING: No SHA-256 command is available for checksum verification.'
fi
record 'WARNING: This source includes a verified Gradle 9.7.0 wrapper JAR, while distributionUrl pins Gradle 9.6.1. Their interoperability is not proven until the --version check succeeds.'

(cd "$ROOT" && sh ./gradlew --no-daemon --console=plain --version) > "$LOGS/GRADLE_VERSION.txt" 2>&1
if [ "$?" -ne 0 ]; then
    record 'INCOMPLETE: Gradle 9.6.1 bootstrap failed. See GRADLE_VERSION.txt (often missing network/distribution).'
    exit 1
fi
if ! grep -F 'Gradle 9.6.1' "$LOGS/GRADLE_VERSION.txt" >/dev/null 2>&1; then
    record 'INCOMPLETE: Bootstrapped Gradle is not version 9.6.1; inspect GRADLE_VERSION.txt.'
    exit 1
fi
record 'PASS: Gradle 9.6.1 bootstrap exited successfully.'

run_task() {
    name=$1
    (cd "$ROOT" && sh ./gradlew --no-daemon --console=plain --no-build-cache --rerun-tasks "$name") > "$LOGS/$(printf '%s' "$name" | tr ':/' '__').txt" 2>&1
    rc=$?
    if [ "$rc" -eq 0 ]; then
        record "PASS: Gradle $name (exit=0)"
        return 0
    fi
    record "INCOMPLETE: Gradle $name (exit=$rc); inspect the task log."
    return 1
}

run_task ':app:clean' || exit 1
run_task ':app:testDebugUnitTest' || exit 1
run_task ':app:assembleDebug' || exit 1
run_task ':app:assemblePreview' || exit 1

for relative in app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/preview/app-preview.apk; do
    file="$ROOT/$relative"
    if ! test -s "$file"; then
        record "INCOMPLETE: APK not found or zero bytes: $relative"
        exit 1
    fi
    record "PASS: nonempty APK exists: $relative"
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$file" >> "$LOGS/APK_SHA256SUMS.txt"
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$file" >> "$LOGS/APK_SHA256SUMS.txt"
    fi
done
record 'PASS: Phase 01 automated Gradle build gates passed on this device.'
record 'DEVICE GATE STILL REQUIRED: manually install and launch ONLY the com.transvibe.app.preview APK; document hardware/OS/ABI, logs, and any crash.'
record 'ASR IS NOT VALIDATED: native diagnostic JNI is not real speech recognition.'
exit 0
