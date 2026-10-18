# WAE — WhatsApp Status Saver

Personal, sideloaded. No internet permission, no ads, no analytics.

## Build

Nothing to configure. Push to `main` and GitHub Actions builds an installable
debug APK, or run it by hand from the Actions tab.

Local build:

```
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

The first CI run generates the Gradle wrapper if `gradlew` is missing, so you do
not need to commit the wrapper jar.

## Requirements

- JDK 17
- Android SDK with compileSdk 35
- Gradle 8.9 (the workflow installs it)

## Install

Download the APK artifact, copy it to the phone, tap it. You will need to allow
"Install unknown apps" for whichever app you use to open it.

## First launch

1. Grant the media permission.
2. Open Settings and switch on **Unrestricted battery** — without it Doze will
   kill the watcher.
3. Switch on **Auto save**.

**All files access** only appears if your phone still uses the old
`/sdcard/WhatsApp/Media/.Statuses` layout. On most Android 11+ devices statuses
live in `Android/media/...`, which needs no permission at all, so the row stays
hidden.

## How detection works

A 3-second poll is the primary path. `FileObserver` on FUSE-backed `/sdcard`
drops events on plenty of ROMs, so it is only used to shorten latency. Every
pass is a full directory reconcile, which means a missed event self-heals on the
next tick instead of being lost.

Duplicates are suppressed by a content fingerprint (length + SHA-256 of the
first 64 KiB), not by filename — WhatsApp reuses filenames when a status is
re-viewed inside the 24 hour window.

## Release signing (optional)

Create a keystore, then add four repository secrets:

```
KEYSTORE_BASE64      base64 -w0 wae-release.jks
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

The workflow then also builds a signed, minified release APK.

## Layout

```
app/src/main/java/com/wae/statussaver/
  WaeApp.kt                 Application, notification channel, image loader
  MainActivity.kt           edge-to-edge, permission gate, VM wiring
  data/                     StatusItem, StatusRepository
  ui/                       Theme, StatusScreen
  vm/                       StatusViewModel
  service/                  AutoSaveService, BootReceiver
  util/                     Perms, HashIndex, StatusDiscovery
```
