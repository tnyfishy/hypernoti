# HyperNoti

Android English/Vietnamese setup assistant for FCM on Chinese HyperOS ROMs.

## Scope of v0.1

- Detect whether Google Play services is installed and enabled.
- Open its app settings, HyperOS autostart settings, battery settings, and application settings.
- Provide a persistent setup checklist and instructions for real remote-push testing.
- Switch between English and Vietnamese; initially follows device language.
- List installed applications locally, including Google Play services and system apps.
- Optional Shizuku user service or explicit `su`/Magisk mode: request a Doze allowlist entry and set `RUN_ANY_IN_BACKGROUND` to `allow` for the selected package.
- Remove the user Doze exemption and reset the app operation to `default`, with an explicit warning that this is not restoration of previous custom settings.
- Open the user-controlled modify-system-settings permission page. That optional permission is not used to bypass privileged access.

The default mode **cannot keep another process alive**, bypass HyperOS restrictions, install Google services, or guarantee FCM delivery. Shizuku/root actions only run after selecting an app and confirming; they change standard Android background policy, not Xiaomi-specific autostart. There is no persistent keep-alive service. The short-lived Shizuku user service is disconnected when leaving advanced settings. If Google services are absent, the checklist alone cannot supply them. Device-specific settings may be unavailable; the app handles this and provides manual instructions. The generic battery page is only an entry point: change each affected app's battery policy manually.

## Development

Requires JDK 17 or 21, Android SDK platform 35 and build-tools 35.0.0. Set `ANDROID_HOME` to the installed SDK.

```sh
./gradlew --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Install on an authorized connected device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Device validation (required before claiming FCM effectiveness)

1. Install and launch on a Chinese HyperOS phone. Check service detection and all settings shortcuts; unavailable shortcuts should show a message.
2. Switch English/Vietnamese, confirm checklist state survives restart, and confirm both translations display properly.
3. Enable the ROM's basic Google services if provided. Configure autostart, unrestricted battery, notifications, and background data for Google Play services and the affected notification apps where supported.
4. Send a real remote push from another device or the affected app's backend. Repeat with screen off, after 15 minutes idle, and after reboot. Record HyperOS version and results. A local notification or an open app is not proof of background FCM delivery.

No Firebase credentials are needed to build this assistant. It does not register its own Firebase project and cannot inspect other apps' FCM tokens or delivery state.

## Advanced setup and limitations

Install and start Shizuku separately (https://shizuku.rikka.app/), then authorize HyperNoti in Advanced settings. Wireless-debugging Shizuku normally must be restarted after each phone reboot. Root mode instead calls the installed `su` binary and respects the root manager's consent prompt. No root operation occurs at startup.

The explicit commands are `cmd deviceidle whitelist +PACKAGE` and `cmd appops set PACKAGE RUN_ANY_IN_BACKGROUND allow`; removal uses `-PACKAGE` and `default`. Shizuku reports each command's exit status. Root uses one `su` request and a command chain: it stops after the first failing command and reports the chain's exit status. Unsupported or denied operations are surfaced, and partial success is possible. Applying twice requests the same desired state. Commands accept only validated package names. They do not edit ROM databases, disable security services, or impersonate another app's Firebase identity. Xiaomi's autostart interface is opened separately because its privileged implementation varies across ROM versions.

`QUERY_ALL_PACKAGES` is needed for the installed-app picker and is restricted by Google Play distribution policy; this initial build is intended for direct APK testing. Application names are not uploaded. The optional `WRITE_SETTINGS` permission applies to HyperNoti itself and is not required for Shizuku/root commands. No Firebase credentials are included.

Additional on-device checks: deny Shizuku permission; stop/restart Shizuku; test command failure; test root denial; confirm command effects in device settings/shell; verify system app selection; test removal without assuming original custom state is restored. Both languages and screen rotation should preserve checklist state. Do not claim HyperOS FCM reliability from host unit tests alone.

## Cloud environment

The prepared instance uses `/workspace/tooling/env.sh` for JDK, SDK, proxy, and writable-cache activation. Source it before invoking the commands above. Installation/start instructions are saved separately in the cloud environment draft; publication is a separate user action.

`QUERY_ALL_PACKAGES` has a narrowly scoped manifest lint exemption because selecting system and background-only apps is the explicit function of this direct-distributed app-management tool. Other lint checks remain enabled. This exemption is not Google Play policy approval.

## Publishing the preview

Pushing an existing `v*` tag starts `.github/workflows/release.yml`. It builds the APK, runs tests and lint, and publishes a GitHub prerelease with the APK and SHA-256 checksum. The workflow can also be started manually with an existing tag. It requires Actions to be enabled and the repository workflow token to have contents-write permission. See RELEASE_NOTES.md for preview limitations.
