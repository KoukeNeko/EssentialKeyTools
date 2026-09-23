## What's changed

- **The haptic feedback levels are now actually different from each other.** Light, Medium and Strong
  used to produce the same buzz — around 37ms in the framework's own vibration log — because the
  Phone (3) vibrator reports no supported predefined effects, so every level fell through to the same
  fallback pattern. Light and Strong are now explicit pulses of 15ms and 80ms, and Medium keeps the
  system click that matches the phone's own buttons. Reported in
  [#4](https://github.com/KoukeNeko/EssentialKeyTools/issues/4).

## Notes for testers

- Please try all four Haptic feedback options and say whether the steps between them are now clear
  enough, and whether **Strong** should be longer or shorter.
- On a Phone (3) the three levels measure roughly 20ms, 37ms and 88ms.
- The vibrator on this device exposes no amplitude control and no Composition primitives, so pulse
  length is the only dimension available: "Strong" means longer, not louder.
- Worth checking that a quick double press still feels like two separate taps rather than one long
  buzz, since Strong is now about five times longer than the pattern it replaced.
