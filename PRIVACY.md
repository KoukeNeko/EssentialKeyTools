# Privacy Policy — Essential Key Tools

_Last updated: 17 July 2026_

Essential Key Tools ("the app") is a free, open-source Android app that remaps the Nothing Phone
Essential Key. This policy explains what the app does — and does not do — with your data.

**In short: the developer does not collect, store, or sell personal information. The app has no
user accounts, analytics, advertising, or tracking. Its only automatic network request is the
public GitHub contributor-list request described below.**

## Data the app collects

The developer collects none. The app has no analytics SDKs, advertising, crash or telemetry
reporting, accounts, or sign-in. It does not send your settings or hardware key-event data anywhere.

## Data stored on your device

Your onboarding progress and settings — which action each gesture (single, double, triple, or long
press) is mapped to, and the learned scan code of your key — are stored locally on your device using
Android's DataStore. This data never leaves your device and is removed when you uninstall the app.

## Data retention, deletion, and security

The local settings described above remain only until you change them, clear the app's storage, or
uninstall the app. The developer has no server-side copy to retain or delete. Android protects these
settings inside the app's private storage, and the app sends none of them over a network.

## Accessibility Service

The app uses an Accessibility Service to receive hardware key events, including the key code, scan
code, press or release state, and event time. These events are processed on your device, including
when the app is not open, to recognise a single, double, triple, or long press of your hardware
Essential Key and run the action you assigned. The service follows only the fixed gesture-to-action
mapping you set; it does not choose or plan actions. If you assign Screenshot or Lock screen, the
service invokes the corresponding Android accessibility system action only after it recognises that
assigned gesture. The service:

- does not read screen content or typed text;
- does not keep a history or log of hardware key events;
- stores only the scan code you explicitly confirm in Key Setup, as described above;
- does not transmit or share hardware key event data.

Android requires an accessibility service to observe hardware keys, which is why the app needs this
permission. Immediately before opening Android's Accessibility Settings, the app presents a separate
in-app disclosure and asks you to choose "Agree: use accessibility service" or "Decline: don't use
accessibility service." Declining or dismissing the disclosure does not enable the service or open
Settings.

## Network access (INTERNET permission)

The app makes one kind of network request: the home screen fetches the public list of the project's
contributors from the GitHub API
(`https://api.github.com/repos/KoukeNeko/EssentialKeyTools/contributors`) to display their names. This
request sends no personal information. As with any web request, GitHub may process your device's IP
address under its own privacy policy. This feature is cosmetic — every other part of the app works
fully offline, and the app still functions if the request fails.

## Permissions used

- **INTERNET** — only to fetch the contributor list described above.
- **Accessibility Service** (`BIND_ACCESSIBILITY_SERVICE`) — to detect your Essential Key press.
- **Package visibility / queries** — to list your launchable apps in the action picker, and to check
  the enabled state of Nothing's Essential Space packages for the unlock wizard.

## Data sharing

The app does not share settings or hardware key-event data with third parties and has no backend
server of its own. Its only automatic third-party connection is the GitHub contributor-list request
described above; it contains no app data, although GitHub receives ordinary connection metadata such
as the device's IP address.

## Children

The app is not directed at children and does not knowingly collect data from anyone.

## Changes to this policy

If this policy changes, the updated version will be published in this repository with a new
"last updated" date above.

## Contact

The developer is KoukeNeko. Questions or privacy requests can be submitted through the public issue
tracker at
<https://github.com/KoukeNeko/EssentialKeyTools/issues>.

---

Essential Key Tools is open-source — you can read the full source code at
<https://github.com/KoukeNeko/EssentialKeyTools> and verify everything described here.
