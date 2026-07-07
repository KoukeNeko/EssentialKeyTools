#!/usr/bin/env bash
#
# verify.sh — the build safety net for EssentialKeyTools.
#
# Runs the full quality gate (lint, unit tests, debug assembly) and prints a single PASS/FAIL
# summary. Exits non-zero on the first failure so it can gate a commit or CI step.
#
# JAVA_HOME defaults to the JBR shipped with Android Studio (the toolchain this project builds with)
# but can be overridden from the environment for other setups:
#   JAVA_HOME=/path/to/jdk ./scripts/verify.sh
#
set -euo pipefail

readonly DEFAULT_JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
readonly GRADLE_TASKS=("lint" "test" "assembleDebug")

# Resolve the repo root from this script's location so it runs from anywhere.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

main() {
  export JAVA_HOME="${JAVA_HOME:-${DEFAULT_JAVA_HOME}}"

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

fail() {
  echo ""
  echo "======================================"
  echo " VERIFY: FAIL — $1"
  echo "======================================"
  exit 1
}

main "$@"
