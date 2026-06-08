# BeatDrop phone install

## Primary parser-hardened APK

Use this first:

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `eabcdd1922c18e3ddda8ba3eccc5415caf4825d75a996de1f79315bd26e28509`
- Source release: `build-37`
- Package: `com.beatdrop.app`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34
- Signed: v1 + v2 + v3 verified by CI

Parser hardening in this build:

- CI verifies APK signature with `apksigner`.
- CI verifies package badging with `aapt`.
- CI fails if package/minSdk/targetSdk are unexpected.
- Release APK is signed with the BeatDrop release certificate, not Android Debug.
- `android:extractNativeLibs="true"` is forced for maximum OEM installer compatibility.

## Stable fallback APK

If the primary APK still cannot parse, try this fallback:

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop-stable-fallback.apk

Fallback expected details:

- File name: `BeatDrop-stable-fallback.apk`
- Size: about `24 MB`
- SHA256: `fd4661129507c8062dc1598760307e8061eb06ca7c7389ee3cfed2ec96726cd9`

## Phone-only install checklist

1. Delete all old BeatDrop APK files from Downloads.
2. Download the APK using Chrome, not the GitHub app preview.
3. Wait until download is fully complete.
4. File size must be about 24 MB.
5. Tap the `.apk` file itself, not `.sha256`.
6. If Android says there is a conflict, uninstall old BeatDrop first.

If both APKs say parsing error, the phone is likely not receiving the real APK bytes or the download manager/file manager is corrupting the file. The server response is `application/octet-stream` and the APK files validate as real APK ZIPs in CI/local checks.
