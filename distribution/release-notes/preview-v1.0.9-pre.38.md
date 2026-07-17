## Essential Key Tools 1.0.9 pre.38

This preview adds a manual, source-aware update check to the home screen.

### Update checks

- Google Play installations use the official Google Play update-availability service and continue
  through the app's Play listing.
- Sideloaded production installations check only stable GitHub Releases.
- Preview installations check only GitHub pre-releases, so a stable build is never offered a test
  version by mistake.
- The app displays the installed version, update source, checking state, result, and retry action in
  English and Traditional Chinese.

### Safe update handoff

- Update checks run only after the user presses the button.
- The app reads release metadata but never downloads or installs an APK itself.
- GitHub release URLs are accepted only from this project's trusted release path.
- The privacy policy and README now document the manual update request and its source selection.

### Preview release automation

- Main-branch CI verifies that the signed annotated Preview tag points to the exact workflow commit.
- CI builds and publishes the pre-release APK instead of relying on a manually uploaded file.
- VirusTotal and Koodous submission results are appended below when their optional CI credentials
  are configured.

### Preview build

- Package: `dev.koukeneko.essentialkeytools.preview`
- Version: `1.0.9-pre.38` (`10009`)
- App label: `Essential Key Tools Preview`

This pre-release installs alongside the production app and does not trigger a Google Play upload.
