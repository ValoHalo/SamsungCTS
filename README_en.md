# SamsungCTS

[简体中文](README.md)

A rootless Google Circle to Search companion for Samsung One UI, derived from [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS), licensed under GPL-3.0.

SamsungCTS configures the bottom-corner assistant gesture and restores it after startup unlock. Google provides the search UI. SamsungCTS has no network access, screen-reading capability or persistent service.

## Setup

1. Install/update Google and select it as the default digital assistant. Confirm Samsung's first-use assistant chooser.
2. Install SamsungCTS and grant the settings permission once. Either start Shizuku 13+ through wireless debugging/ADB and select **Authorize with Shizuku**, or enable USB debugging and run this command on a computer:

   ```sh
   adb shell pm grant dev.valohalo.samsungcts android.permission.WRITE_SECURE_SETTINGS
   ```

3. Set SamsungCTS to **Unrestricted** in App info → Battery / Background activity, including the confirmation dialog. Restricted background mode can cause Samsung to skip boot broadcasts.
4. Use swipe gesture navigation, then select **Enable corner gesture** in SamsungCTS.
5. Swipe inward from either bottom corner, use the quick settings tile, or long-press the app icon. The center navigation-bar hold retains its original behavior.

If an older standalone gesture helper is installed, restore its original settings first. SamsungCTS does not require the original MiCTS app. Restore original settings before uninstalling.

## Scope and implementation

Android 13+; setup is specific to Samsung One UI. The Binder wire format is checked against AOSP Android 13–17 and Samsung S23 Android 16 / S24 Android 17 frameworks. Google and firmware support still determine actual availability. See [validation](docs/VALIDATION.md).

This fork removes Xposed/LSPosed hooks, module metadata, device spoofing and all associated dependencies, including `hiddenapibypass`. It uses the system Binder protocol without hidden-API exemptions. It cannot guarantee how other applications classify a device.

The app requests `WRITE_SECURE_SETTINGS`, `RECEIVE_BOOT_COMPLETED`, and optional Shizuku client authorization. Shizuku is needed only for the initial grant, not daily operation or boot recovery. Its short-lived user service accepts only ADB identity and grants only this application's fixed settings permission; it exposes no general command runner and is removed on completion or timeout.

The custom binder provider does not initialize Sui. Both APK variants are shrunk and checked to ensure unused Sui classes are removed. Normal Shizuku client metadata remains visible. The app backs up the assistant, voice interaction service and navigation flags before changing them. Startup recovery runs once with a single delayed recheck. There is no GMS policy override or network proxy in this app.

## Build and license

Use JDK 17 and Android SDK 36:

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Debug APKs are under `app/build/outputs/apk/debug/`. Use a stable private signing key for releases; do not commit signing keys or passwords. CI uploads debug artifacts without publishing releases.

The original Circle to Search invocation approach, tile and some resources come from MiCTS by parallelcc. Samsung-specific setup and recovery are added by this fork. Upstream Git history and [GPL-3.0](LICENSE) are retained. Shizuku API and binder transport are by RikkaW under the retained [MIT license](app/src/main/assets/SHIZUKU-LICENSE.txt). Distributions must include the corresponding source and licenses.
