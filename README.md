<p align="center">
  <img src="logo/github_social_preview.png" alt="Essential Key Tools" width="100%">
</p>

<h1 align="center">Essential Key Tools</h1>

<p align="center">
  <strong>Your Essential Key, your shortcuts.</strong><br>
  Turn single, double, triple, and long presses into the actions you actually use.
</p>

<p align="center">
  <a href="https://github.com/KoukeNeko/EssentialKeyTools/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/KoukeNeko/EssentialKeyTools?style=for-the-badge&logo=github&label=RELEASE&color=2196F3"></a>
  <a href="https://github.com/KoukeNeko/EssentialKeyTools/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/KoukeNeko/EssentialKeyTools/total?style=for-the-badge&logo=github&label=DOWNLOADS&color=4CAF50"></a>
  <img alt="Android 15+" src="https://img.shields.io/badge/ANDROID-15%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <a href="https://github.com/KoukeNeko/EssentialKeyTools/stargazers"><img alt="Stars" src="https://img.shields.io/github/stars/KoukeNeko/EssentialKeyTools?style=for-the-badge&logo=github&label=STARS&color=2196F3"></a>
</p>

<p align="center">
  <a href="https://github.com/KoukeNeko/EssentialKeyTools/releases/latest"><strong>Download the latest release</strong></a>
  · <a href="#getting-started">Getting started</a>
  · <a href="PRIVACY.md">Privacy</a>
</p>

Essential Key Tools gives the Nothing Phone's **Essential Key** a life beyond its default action.
Assign a different shortcut to each gesture and use it instantly, without opening the app first.

It is built to keep you in control: no root access or computer setup is required. The app explains
its accessibility use before Android settings open, provides clear agree and decline choices, and
never treats leaving the page as consent.

## See it in action

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="19%" alt="Home screen" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="19%" alt="Action picker" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="19%" alt="Unlock wizard" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="19%" alt="Key setup" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="19%" alt="Key test" />
</p>

## Make one key do more

### Four gestures, four shortcuts

Give single press, double press, triple press, and long press their own actions. Launch an app,
toggle the flashlight, take a screenshot, lock the screen, control media, or change the ringer mode.

### Take back the single press

Nothing OS normally reserves a single press for Essential Space or Recorder. The built-in unlock
wizard shows the relevant packages and opens each App Info page so you can disable or restore them
manually. Double, triple, and long press work without this optional step.

> **Before you unlock:** freeing the single press disables Nothing's Essential Space and Recorder
> entirely. You can restore them from the same App Info pages, and an OS update may enable them
> again.

### Test without surprises

Key Test shows the detected hardware events and recognized gesture without running the shortcut
assigned to it. You can confirm everything works before relying on a mapping.

### Stay up to date on your terms

Update checks happen only when you press **Check for updates**. Google Play installations stay with
Google Play, sideloaded stable builds check stable GitHub Releases, and Preview builds check only
pre-releases. The app never downloads or installs an APK by itself.

## A clear choice before accessibility

The first-run guide starts with language selection and a short introduction. Its final page explains:

- why Essential Key detection needs Android's accessibility service;
- which hardware-key information the app receives and how it is used;
- what is stored on the device;
- when screenshot or lock-screen actions can run; and
- exactly what happens after either choice.

Two separate actions are always shown:

- **Agree: use accessibility service** opens Android accessibility settings.
- **Decline: don't use accessibility service** finishes setup without enabling the service.

Pressing Back or leaving the disclosure never enables accessibility. You can review the guide and
change your choice later from the home screen.

## Privacy by design

- Hardware-key events are processed immediately on the device and are not stored, uploaded, or
  shared.
- Only the Essential Key identifier confirmed in Key Setup is saved locally.
- The app does not read screen content, text fields, or text you type.
- Screenshot and lock-screen actions run only when you explicitly assign them to a gesture.
- There are no accounts, analytics, ads, or tracking.

Read the complete [Privacy Policy](PRIVACY.md).

## Getting started

1. **Choose your language** and read the short product introduction.
2. **Review the accessibility disclosure** and choose whether to use the service.
3. **Learn your key** from Home → *Key setup*, then press the Essential Key and save it.
4. **Choose your shortcuts** by tapping each gesture on the home screen.
5. **Test the gestures** in *Key Test* without executing their mapped actions.
6. **Optionally free single press** with the *Unlock wizard* if you do not use Essential Space or
   Recorder.

## Compatibility

- Android 15 or newer
- Nothing phones with an Essential Key
- English and Traditional Chinese

The app learns the key identifier at runtime instead of assuming every model or firmware reports the
same value. Availability of the system-app **Disable** button for the optional single-press unlock
depends on the Nothing OS build.

## Get the app

Download the signed APK from the
[latest GitHub Release](https://github.com/KoukeNeko/EssentialKeyTools/releases/latest). Every stable
release includes its APK SHA-256 digest plus VirusTotal and Koodous submission status. Preview builds
use a separate application ID and can be installed beside the stable app.

---

## Technical reference

### How Essential Key detection works

The Essential Key enters the Android input pipeline as `keyCode=0`; current verified devices report
Linux `scanCode=250`. Because the public key-layout files leave it unmapped, the accessibility
service can observe the event while the app filters for the identifier learned in Key Setup. The
gesture classifier then resolves single, double, triple, or long press and runs the selected action.

Nothing OS also handles the default single press in system policy. Consuming the accessibility key
event does not stop that system action, which is why the separate manual unlock flow is needed for
single-press remapping.

### Permissions and network access

- **Accessibility service** - observes hardware-key press and release events, filters them to the
  learned Essential Key, classifies the gesture, and runs the mapped action. The service does not
  process window or screen-content events.
- **`INTERNET`** - fetches the public GitHub contributor list for the contribution card. A separate
  request runs only after **Check for updates** is pressed and contacts Google Play or GitHub based
  on the verified installation source.

### Development

Install Android Studio with the Android SDK and JDK 21. Android Studio's bundled JBR works.

macOS / Linux:

```bash
./gradlew lint test assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat lint test assembleDebug
```

On macOS and Linux, the verification harness runs the same quality gate and prints a single
PASS/FAIL summary. It uses `JAVA_HOME` when set, otherwise it detects Android Studio's macOS JBR or
the Java installation on `PATH`:

```bash
./scripts/verify.sh
```

`scripts/simulate-key.sh` is a best-effort development helper that injects a scancode-250 event with
`adb sendevent`. It needs the correct input node and usually root on stock firmware; the app itself
does not require root, Shizuku, ADB, or this development setup.

Pure logic such as gesture classification, settings serialization, unlock-status mapping, and
update-version parsing is covered by JVM unit tests without requiring a device.

<p>
  <a href="https://github.com/KoukeNeko/EssentialKeyTools/actions/workflows/build-apk.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/KoukeNeko/EssentialKeyTools/build-apk.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=BUILD"></a>
  <img alt="Kotlin" src="https://img.shields.io/badge/KOTLIN-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/COMPOSE-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/LICENSE-MIT-4CAF50?style=for-the-badge&logo=github"></a>
</p>

## Support

If Essential Key Tools is useful to you, you can support development:

<a href="https://buymeacoffee.com/doershing"><img alt="Buy Me a Coffee" src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-doershing-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black"></a>

## License

[MIT](LICENSE) © KoukeNeko
