#!/usr/bin/env bash
set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Refusing non-emulator device' >&2; exit 1; }
[[ "$(adb -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || { echo 'Device is not an emulator' >&2; exit 1; }
hardware="$(adb -s "$serial" shell getprop ro.hardware | tr -d '\r')"
[[ "$hardware" == ranchu || "$hardware" == goldfish ]] || { echo 'Unexpected emulator hardware' >&2; exit 1; }
[[ "$(adb -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || { echo 'Emulator is not booted' >&2; exit 1; }
export ANDROID_SERIAL="$serial"
adb -s "$serial" shell input keyevent 82
mkdir -p ci-reports
for test_class in ReportExpenseDetailTest ResponsiveChartTest CategoryDrilldownTest; do
    (cd android && bash ./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.personalfinance.tracker.$test_class" --no-daemon)
    python3 scripts/ci/summarize-test-results.py \
      --results android/app/build/outputs/androidTest-results/connected/debug \
      --allowed-skips scripts/ci/allowed-skips.json --profile pr --expected-class "com.personalfinance.tracker.$test_class" \
      --summary "ci-reports/$test_class.md"
    cp -R android/app/build/outputs/androidTest-results/connected/debug "ci-reports/$test_class-results"
done
