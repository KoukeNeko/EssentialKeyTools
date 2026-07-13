# Privacy Policy — Essential Key Tools

_Last updated: 13 July 2026_

Essential Key Tools ("the app") is a free, open-source Android app that remaps the Nothing Phone
Essential Key. This policy explains what the app does — and does not do — with your data.

**In short: the app does not collect, store, or share any personal information. It has no user
accounts, no analytics, no advertising, and no tracking.**

## Data the app collects

None. The app does not collect, transmit, or sell any personal or usage data. There are no analytics
SDKs, no advertising, no crash/telemetry reporting, and no accounts or sign-in.

## Data stored on your device

Your settings — which action each gesture (single, double, triple, or long press) is mapped to, and
the learned scan code of your key — are stored locally on your device using Android's DataStore. This
data never leaves your device and is removed when you uninstall the app.

## Accessibility Service

The app uses an Accessibility Service for one purpose: to detect a press of your hardware Essential
Key so it can run the action you assigned to it. The service receives hardware key events only in
order to recognise your Essential Key; it:

- ignores every other key and input;
- does not read screen content, capture text, or log or store keystrokes;
- transmits nothing off your device.

Android requires an accessibility service to observe hardware keys, which is why the app needs this
permission.

## Network access (INTERNET permission)

The app makes one kind of network request: the home screen fetches the public list of the project's
contributors from the GitHub API
(`https://api.github.com/repos/KoukeNeko/EssentialKeyTools/contributors`) to display their names. This
request sends no personal information. As with any web request, GitHub may process your device's IP
address under its own privacy policy. This feature is cosmetic — every other part of the app works
fully offline, and the app still functions if the request fails.

## Shizuku

If you use the one-tap options (enabling the accessibility service, or freeing the single press), the
app communicates with Shizuku — a separate app that you install and control — to run privileged shell
commands locally on your device. No data is sent off the device. Shizuku is a third-party app governed
by its own privacy policy.

## Permissions used

- **INTERNET** — only to fetch the contributor list described above.
- **Accessibility Service** (`BIND_ACCESSIBILITY_SERVICE`) — to detect your Essential Key press.
- **Package visibility / queries** — to list your launchable apps in the action picker, and to check
  the enabled state of Nothing's Essential Space packages for the unlock wizard.
- **Shizuku provider** — to receive the privileged binder from Shizuku when you use its one-tap options.

## Data sharing

The app does not share any data with third parties. It has no backend server of its own.

## Children

The app is not directed at children and does not knowingly collect data from anyone.

## Changes to this policy

If this policy changes, the updated version will be published in this repository with a new
"last updated" date above.

## Contact

Questions about this policy? Please open an issue at
<https://github.com/KoukeNeko/EssentialKeyTools/issues>.

---

Essential Key Tools is open-source — you can read the full source code at
<https://github.com/KoukeNeko/EssentialKeyTools> and verify everything described here.
