## What's changed

- Added **Next track** and **Previous track** actions, so a gesture can skip through what you are
  listening to without unlocking the phone. Requested in [#1](https://github.com/KoukeNeko/EssentialKeyTools/issues/1).
- Added a **Diagnostics** screen that shows the last crash the app caught, ready to copy or share
  straight into a bug report.
- Fixed a crash when the ringer-cycle action ran while the phone was already silent.
- Fixed a crash in the action picker caused by an installed app with an oversized icon.
- Added a shortcut to the Do Not Disturb access screen, which the ringer cycle needs before it can
  reach silent mode.

## About the Diagnostics screen

Crash reports stay on your device. There is no crash-reporting service and nothing is uploaded — the
app has no server to upload to. A report only leaves your device if you copy or share it yourself,
and you can read all of it first.

The report carries the app version, how the app was installed, your device model, your Android
version, and the technical stack trace. It deliberately carries nothing the accessibility service
observes: no screen content, no notification text, and no record of which keys you pressed.

## Notes for testers

- The media actions have not been verified on a physical device yet — please try them against your
  usual player and report anything odd.
- "Previous track" does what your player decides it means. Most players restart the current track
  instead of going back once it has been playing for a few seconds; that is the player's behaviour,
  not the app's.
