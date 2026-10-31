# Validation

Validated on 2026-10-31 against a stock Samsung S23 SM-S9110, CHC, Android 16 / One UI 8.5, firmware S9110ZCS8FZG1. These results refer to this fork's APK, not only the preceding standalone proof of concept.

## Results

- `assembleDebug` and `lintDebug`: passed.
- `assembleRelease` and `lintRelease`: passed; 0 lint errors, 18 non-blocking warnings (dependency updates, style suggestions and existing icon guidance).
- Signed release APK: 2,079,251 bytes; no debuggable flag; APK signature verification passed.
- APK module-artifact audit: passed for both debug and signed release.
- Direct Binder launch from an ordinary app process: opened Google's Circle to Search before WRITE_SECURE_SETTINGS was granted to the debug app.
- Release installation, one-time settings grant, and Samsung Unrestricted background setting: passed.
- Independent corner gesture: opened Google search through `dev.valohalo.samsungcts`, without using the original MiCTS app.
- Restore action: restored the saved Google assistant component, empty voice interaction service and navigation flags 4. Re-enable applied this app's activity, Google VIS and flags 12.
- Restart: after unlocking, with no manual application or enable action, the boot receiver was delivered and the three settings were restored automatically. The finite recheck completed normally.
- Existing GMS policy helper remained at policy bits 0. No GMS override is included in SamsungCTS.

Signed release SHA-256:

```text
4252b40089e7124819765d53f1916b8cdfb3d58da2e02741e4b5db01c90e9d55
```

The private signing key is kept outside this repository. The original MiCTS app was not uninstalled; it is no longer part of this fork's invocation path. The previous standalone gesture helper was restored and disabled before migration.

Other models, account/server eligibility and later Google/system updates have not received equivalent runtime testing. Protocol inspection alone is not a device-compatibility guarantee.

## Binder protocol

`IVoiceInteractionManagerService.showSessionFromSession` is transaction 3 in AOSP Android 13–17 and the inspected Samsung S23 Android 16 / S24 Android 17 frameworks. The request contains the interface token, nullable session binder, typed Bundle and integer flags. Android 14+ appends the nullable attribution tag. The reply is an exception header followed by a boolean.

Sources:

- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-release/core/java/com/android/internal/app/IVoiceInteractionManagerService.aidl
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/core/java/com/android/internal/app/IVoiceInteractionManagerService.aidl
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/com/android/internal/app/IVoiceInteractionManagerService.aidl
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/com/android/internal/app/IVoiceInteractionManagerService.aidl

Only ServiceManager.getService is reflected. The blocked interface method is not reflected, and no hidden-API exemption is requested. Unknown API levels fail closed rather than sending an unverified transaction.

## APK audit

Run `python scripts/check_apk.py app/build/outputs/apk/debug/app-debug.apk`. The check scans executable DEX, compiled XML/resources and metadata for removed module namespaces and entry points. This is a bounded artifact check, not a promise about how arbitrary third-party applications classify the device.
