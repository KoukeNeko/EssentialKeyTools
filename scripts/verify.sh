#!/usr/bin/env bash
#
# verify.sh — the build safety net for EssentialKeyTools.
#
# Runs the full quality gate (lint, unit tests, debug assembly) and prints a single PASS/FAIL
# summary. Exits non-zero on the first failure so it can gate a commit or CI step.
#
# JAVA_HOME can be provided explicitly. Otherwise the script uses Android Studio's standard macOS
# JBR location when available, then falls back to the Java installation on PATH:
#   JAVA_HOME=/path/to/jdk ./scripts/verify.sh
#
set -euo pipefail

readonly MACOS_ANDROID_STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
readonly GRADLE_TASKS=("lint" "test" "assembleDebug")

# Resolve the repo root from this script's location so it runs from anywhere.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly SCRIPT_DIR PROJECT_ROOT

main() {
  local resolved_java_home
  if ! resolved_java_home="$(resolve_java_home)"; then
    fail "JDK 21 was not found. Set JAVA_HOME or add Java to PATH."
  fi
  export JAVA_HOME="${resolved_java_home}"

  if [[ ! -d "${JAVA_HOME}" ]]; then
    fail "JAVA_HOME does not exist: ${JAVA_HOME}"
  fi

  echo "==> JAVA_HOME=${JAVA_HOME}"
  echo "==> Running: ./gradlew ${GRADLE_TASKS[*]}"

  if (cd "${PROJECT_ROOT}" && ./gradlew "${GRADLE_TASKS[@]}"); then
    echo ""
    echo "======================================"
    echo " VERIFY: PASS"
    echo "======================================"
    exit 0
  fi

  fail "Gradle build failed — see the log above."
}

resolve_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    printf '%s\n' "${JAVA_HOME}"
    return
  fi

  if [[ -d "${MACOS_ANDROID_STUDIO_JBR}" ]]; then
    printf '%s\n' "${MACOS_ANDROID_STUDIO_JBR}"
    return
  fi

  if command -v java >/dev/null 2>&1; then
    java -XshowSettings:properties -version 2>&1 \
      | awk -F '= ' '/^[[:space:]]*java.home = / { print $2; exit }'
    return
  fi

  return 1
}

fail() {
  echo ""
  echo "======================================"
  echo " VERIFY: FAIL — $1"
  echo "======================================"
  exit 1
}

main "$@"
