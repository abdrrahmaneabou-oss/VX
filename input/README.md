# APK input

Place the original, unmodified `V2F.apk` in this directory with the exact path:

`input/V2F.apk`

Pushing that file triggers `.github/workflows/build-apk.yml` automatically.

The workflow does not raw-patch the ZIP. It decodes the APK with Apktool, replaces the blocked-label array in `SettingsClickGuardConfig`, rebuilds, zipaligns, signs, verifies the APK signature, and uploads `V2F-patched.apk` as the `V2F-patched` Actions artifact.

Current requested label changes:

- Removed: `Private DNS`
- Removed: `Reset network settings`
- Added: `BlockerHero`
- Added: `Reset options`
- Added: `Add a language`
- Added: `Reset app preferences` (correct spelling)
- Added: `Restore Applications`

Note: if repository signing secrets are not configured, CI uses a temporary signing key. An APK signed by a different key must be uninstalled before installing the new build.
