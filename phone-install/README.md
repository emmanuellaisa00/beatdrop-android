# BeatDrop phone install

Use this direct APK file from your phone if GitHub Releases or Actions artifacts download incorrectly.

## Direct APK link

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details for the current debug-log build:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `665456ac075f13fa512aab9838a1f9984b2b35ef74e048ad91e4c3733ba0888c`
- Package: `com.beatdrop.app`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34

Online playback debug logs:

1. Search and tap an online song.
2. Now Playing opens instantly.
3. While it says `Loading audio…`, tap `Copy logs`.
4. If playback fails, tap `Copy logs` beside Retry.
5. Paste/send the copied text so the resolver failure can be diagnosed.

This build logs:

- tapped track title/artist/id/onlineId
- stream resolve start/failure/success
- YouTube player JS cipher warmup
- every Innertube client tried
- HTTP status from `/player`
- playability status/reason
- format and audio candidate counts
- cipher/url resolution failures
- WebView fallback result
- Media3 playback errors after URL submission

Also includes previous UX changes: local Home, green local checkmarks, online-only Search filters, recent searches, Now Playing retry, dark no-cover fallback, auth password visibility, cloud Library onboarding, and backend `external_items` client code.
