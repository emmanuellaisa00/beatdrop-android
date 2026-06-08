# BeatDrop phone install

## Primary APK: parser-hardened build 40

Use this first:

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `fd868225f23ab1a6411e215cb82478e2dd7b7696fd852ffc3e877ec95c8cbfb6`
- Source release: `build-40`
- Package: `com.beatdrop.app`
- Version: `1.0.6` / versionCode `7`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34
- Signed: v1 + v2 + v3 verified by CI

Parser hardening:

- CI verifies APK signature with `apksigner`.
- CI verifies package badging with `aapt`.
- CI fails if package/minSdk/targetSdk are unexpected.
- Release APK is signed with the BeatDrop release certificate, not Android Debug.
- Native libs use legacy packaging via Gradle for OEM installer compatibility.

## Fallback APK: last phone-confirmed build

If build 40 cannot parse on your phone, use this fallback:

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop-stable-fallback.apk

Fallback expected details:

- File name: `BeatDrop-stable-fallback.apk`
- Size: about `24 MB`
- SHA256: `eabcdd1922c18e3ddda8ba3eccc5415caf4825d75a996de1f79315bd26e28509`
- Source release: `build-37`

## Phone-only install checklist

1. Delete all old BeatDrop APK files from Downloads.
2. Download with Chrome, not the GitHub app preview.
3. Wait until download is fully complete.
4. File size must be about 24 MB.
5. Tap the `.apk` file itself, not `.sha256`.
6. If Android reports a conflict, uninstall old BeatDrop first.
