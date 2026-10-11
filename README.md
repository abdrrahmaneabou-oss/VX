# VX — V2F Android App

This repository is initialized from analysis of the supplied `V2F.apk` before any functional modifications.

- Package: `com.v1f.app`
- Version: `2.2.0-F`
- SHA-256: `e66d7650dfcd98962206faca33d37d7bc464958fd865a9af2149bcb65e268134`
- Stack detected: Android / Kotlin / Jetpack Compose

## Main components detected

- `com.v1f.app.V1FApplication`
- `com.v1f.app.MainActivity`
- `com.v1f.app.LockActivity`
- `com.v1f.app.patch.DurationActivity`
- `com.v1f.app.service.AppBlockAccessibilityService`
- `com.v1f.app.service.BackgroundProtectionService`
- `com.v1f.app.admin.V1FDeviceAdminReceiver`
- `com.v1f.app.receiver.BootReceiver`

## Protection architecture detected

- `ProtectionEngine` / `ProtectionModule`
- `AppGuardModule`
- `WebsiteGuardModule`
- Settings click guard modules
- `MeteorShieldOverlay` / `MeteorShieldView`
- Session, permissions, installed-apps, and blocked-sites repositories
- Timer / duration / break runtime logic

## Capabilities detected

- Accessibility Service
- Device Admin
- Foreground service + special-use foreground service
- Notifications
- Query all packages
- Boot completed receiver
- Request to ignore battery optimizations

No functional changes have been made yet.
