# APK structure — V2F 2.2.0-F

## Identity
- Package: `com.v1f.app`
- Version name: `2.2.0-F`
- SHA-256: `e66d7650dfcd98962206faca33d37d7bc464958fd865a9af2149bcb65e268134`

## Application classes detected

### Core
- `com.v1f.app.AppGraph`
- `com.v1f.app.V1FApplication`
- `com.v1f.app.MainActivity`
- `com.v1f.app.LockActivity`

### Data/model
- `BlockedDomainMatcher`
- `BlockedSitesRepository`
- `DomainNormalizer`
- `InstalledAppsRepository`
- `PermissionRepository`
- `SessionRepository`
- `ActiveSession`
- `InstalledApp`
- `PermissionSnapshot`

### Protection
- `ProtectionEngine`
- `ProtectionEngineState`
- `ProtectionModule`
- `AppGuardModule`
- `WebsiteGuardModule`
- `SettingsClickDetector`
- `SettingsClickGuardConfig`
- `SettingsClickGuardModule`
- `SettingsClickMatcher`
- `SettingsEscapeGuard`
- `MeteorShieldOverlay`
- `MeteorShieldView`

### Services / receivers
- `AppBlockAccessibilityService`
- `BackgroundProtectionService`
- `V1FDeviceAdminReceiver`
- `BootReceiver`

### Session-duration patch layer
- `BreakRules`
- `BreakRuntime`
- `BreakStatusView`
- `DurationActivity`
- `DurationRules`
- `RememberedApps`

### UI screens
- `HomeScreenKt`
- `AppSelectionScreenKt`
- `WebsiteSelectionScreenKt`
- `PermissionsScreenKt`
- `TimerScreenKt`
- `MeteorLockScreenKt`

## Manifest capabilities detected
- `android.permission.BIND_ACCESSIBILITY_SERVICE`
- `android.permission.BIND_DEVICE_ADMIN`
- `android.permission.DUMP`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.QUERY_ALL_PACKAGES`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

The supplied artifact contains 14 DEX files (`classes.dex` through `classes14.dex`) plus Android resources and native AndroidX graphics-path libraries for common ABIs.

No functional modifications were made during this inspection.
