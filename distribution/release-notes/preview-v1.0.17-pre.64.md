## What's changed

- **Medium haptic feedback now vibrates on every device.** Medium was the last level still using the
  system's predefined click effect, which produces no vibration at all on a Phone (4a), so Medium felt
  the same as Off there. It is now an explicit 40ms pulse, between Light (15ms) and Strong (80ms).
  Reported in [#4](https://github.com/KoukeNeko/EssentialKeyTools/issues/4).
- **New option: Vibrate only when an action runs.** It sits under the Haptic feedback levels and is off
  by default, so nothing changes unless you turn it on. When it is on, the key vibrates once when a
  gesture runs its action instead of on every press: a double or triple press gives one vibration,
  a long press vibrates when it triggers, and a gesture set to None never vibrates. Requested in
  [#4](https://github.com/KoukeNeko/EssentialKeyTools/issues/4).

## Notes for testers

- On a Phone (4a), please check that Medium now vibrates and sits clearly between Light and Strong.
- With the new option on, a single press vibrates only after the double-press window closes (about
  400ms) whenever a double or triple press is also mapped, because until then the app cannot tell a
  single press from the start of a double press.
