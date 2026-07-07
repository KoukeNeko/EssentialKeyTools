#!/usr/bin/env bash
#
# simulate-key.sh — best-effort device-side helper to fake an Essential Key press for testing.
#
# It finds the input event device, then injects a scancode-250 key down + up via `sendevent` so the
# accessibility service's classifier can be exercised without the physical key.
#
# IMPORTANT CAVEATS (why this is a convenience harness, not CI):
#   * `sendevent` needs the CORRECT /dev/input/eventN node for the key's controller. This script
#     probes `getevent -pl` for a device that reports scancode 250 (0xfa); if it cannot identify one
#     it prints instructions instead of guessing.
#   * On stock (non-root) Nothing firmware, writing to /dev/input typically requires root. Without
#     it, injection will fail with a permission error — that is expected. Use the in-app KEY TEST
#     screen with the real key for the authoritative check.
#
# Usage:
#   ./scripts/simulate-key.sh [ADB_SERIAL]
#
set -euo pipefail

# Essential Key scancode, firmware-verified on Phone (3a)/(3a) Lite (see docs/RESEARCH.md).
readonly ESSENTIAL_KEY_SCANCODE_DEC=250
readonly ESSENTIAL_KEY_SCANCODE_HEX="fa"

# Linux input-event protocol constants (from <linux/input-event-codes.h>).
readonly EV_KEY=1
readonly EV_SYN=0
readonly SYN_REPORT=0
readonly KEY_DOWN=1
readonly KEY_UP=0

main() {
  local adb_serial="${1:-}"
  local adb_target=()
  if [[ -n "${adb_serial}" ]]; then
    adb_target=(-s "${adb_serial}")
  fi

  require_adb
  require_device "${adb_target[@]}"

  local event_device
  event_device="$(find_event_device "${adb_target[@]}")"

  if [[ -z "${event_device}" ]]; then
    print_manual_instructions
    exit 0
  fi

  echo "==> Using input device: ${event_device}"
  inject_key "${event_device}" "${adb_target[@]}"
  echo "==> Injected scancode ${ESSENTIAL_KEY_SCANCODE_DEC} down/up. Watch the app's KEY TEST screen."
}

require_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb not found on PATH. Install platform-tools." >&2
    exit 1
  fi
}

require_device() {
  if ! adb "$@" get-state >/dev/null 2>&1; then
    echo "ERROR: no device reachable via adb. Connect a device and enable USB/wireless debugging." >&2
    exit 1
  fi
}

# Scans `getevent -pl` output for a device whose key bitmap advertises scancode 250 (0x00fa).
find_event_device() {
  adb "$@" shell getevent -pl 2>/dev/null | awk '
    /^add device/ { device = $NF }
    /00fa/        { if (device != "") { print device; exit } }
  '
}

inject_key() {
  local device="$1"
  shift
  adb "$@" shell "sendevent ${device} ${EV_KEY} ${ESSENTIAL_KEY_SCANCODE_DEC} ${KEY_DOWN}"
  adb "$@" shell "sendevent ${device} ${EV_SYN} ${SYN_REPORT} 0"
  adb "$@" shell "sendevent ${device} ${EV_KEY} ${ESSENTIAL_KEY_SCANCODE_DEC} ${KEY_UP}"
  adb "$@" shell "sendevent ${device} ${EV_SYN} ${SYN_REPORT} 0"
}

print_manual_instructions() {
  cat <<EOF
Could not auto-detect the Essential Key input device (scancode ${ESSENTIAL_KEY_SCANCODE_DEC}/0x${ESSENTIAL_KEY_SCANCODE_HEX}).

To inject manually:
  1. List devices:   adb shell getevent -pl
  2. Find the node (e.g. /dev/input/event3) whose events include KEY 00fa.
  3. Inject down + up:
       adb shell sendevent <node> ${EV_KEY} ${ESSENTIAL_KEY_SCANCODE_DEC} ${KEY_DOWN}
       adb shell sendevent <node> ${EV_SYN} ${SYN_REPORT} 0
       adb shell sendevent <node> ${EV_KEY} ${ESSENTIAL_KEY_SCANCODE_DEC} ${KEY_UP}
       adb shell sendevent <node> ${EV_SYN} ${SYN_REPORT} 0

Note: /dev/input access usually needs root on stock firmware. The most reliable test is pressing the
physical key with the app's KEY TEST screen open.
EOF
}

main "$@"
