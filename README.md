# Essential Key Tools

Remap the Nothing Phone **Essential Key** to your own actions — no root required. The hardware key
enters the input pipeline as `keyCode=0` with Linux `scanCode=250`, which an `AccessibilityService`
can observe. Single / double / triple press and long press each get their own action.

The UI follows the Nothing OS design language: pure-black canvas, flat rounded cards with hairline
outlines, uppercase monospace section labels, and red used at most once per screen.

## Features

- **Four gestures, four actions** — single, double, triple, long press, each mapped independently.
- **Built-in actions** — launch app, toggle flashlight, take screenshot, lock screen, play/pause
  media, cycle ringer mode.
- **Runtime scanCode learning** — a "press your key" setup flow captures the scanCode instead of
  hard-coding `250`, so it adapts to any model or firmware.
- **Single-press unlock wizard** — Nothing OS owns the single press until its consumer packages are
  disabled. The wizard frees it two ways: **Shizuku** (on-device, no PC) or **ADB** (copyable
  commands), with one-tap restore and live per-package status.
- **Live status** — home screen shows whether the accessibility service is running and whether the
  single press is freed, re-checked on resume to catch drift from an OS update.

## Setup

1. **Enable the accessibility service** — Home → *Enable service* opens the system settings; turn on
   *Essential Key detection*. It listens only for your hardware key.
2. **Learn your key** — Home → *Key setup* → press the Essential Key → save the captured scanCode.
   Use *Key test* to confirm gestures are classified correctly.
3. **Map actions** — tap any gesture card on the home screen to assign its action.
4. **(Optional) Free the single press** — Home → *Unlock wizard*:
   - **Shizuku path** — install & start [Shizuku](https://shizuku.rikka.app), grant permission,
     tap *Free single press*. *Restore* re-enables the packages.
   - **ADB path** — from a PC:
     ```
     adb shell pm disable-user --user 0 com.nothing.ntessentialspace
     adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder
     ```
     Revert with `pm enable <pkg>`. The wizard shows the exact copyable commands.

   Freeing the single press disables Nothing's Essential Space and Recorder entirely; an OS update
   may re-enable them. Double / triple / long press work without unlocking.

## Development

Build with the JBR shipped in Android Studio:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint test assembleDebug
```

Or use the verification harness (prints a PASS/FAIL summary, `JAVA_HOME` overridable):

```bash
./scripts/verify.sh
```

`scripts/simulate-key.sh` is a best-effort helper that injects a scancode-250 event via
`adb sendevent` for on-device testing (needs the correct input node and usually root on stock
firmware — see the script's header).

Pure logic (gesture classifier, settings serialization, unlock status/command mapping) is covered by
JVM unit tests and has no Android dependency, so it is verified without a device.

## Background

The interception mechanism, package-disable approach, and design tokens are documented in
[`docs/RESEARCH.md`](docs/RESEARCH.md).
