# BeatDrop phone install

Use this direct APK file from your phone if GitHub Releases or Actions artifacts download incorrectly.

## Direct stable APK link

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details for this stable build:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `37c4f32eae31cf61a141a65c7b97b62253a75275d522106ec9ee3c9d054e9455`
- Package: `com.beatdrop.app`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34

This direct APK is intentionally pinned to the last stable phone-install build while newer backend-schema APK parsing is investigated.

Included stable UX changes:

- Home is local/on-device music first.
- Local songs show a Spotify-style green checkmark.
- Home has a Songs list and an Explore your catalogue section.
- Search is online-only with Songs, Albums, Playlists, Artists filters.
- Search header/title/icon stay pinned while scrolling.
- Recent online searches are saved locally and shown on Search.
- Tapping an online song opens Now Playing instantly; tapping the currently playing song again does not restart it.
- If online playback fails, Now Playing shows an error and Retry button instead of spinning forever.
- Global online-loading spinner overlay removed; Now Playing shows inline loading text instead.
- Dark default fallback cover art is used when music has no cover, instead of colorful gradient icons.
- Liked Songs and Downloads tiles use heart/download icons.
- Auth password field has a show/hide password toggle.
- Album detail has an Apple Music-style blurred artwork background.
- Library is online/cloud only: first access prompts sign in, explains cloud/LAISACORP terms, then asks users to pick at least 3 artists for suggestions.
- Artist onboarding picks are saved locally and displayed in Library.
- Guest Home/Library no longer says Alex; signed-in users show their display name.

Backend-schema integration is in source code, but not pinned to this stable direct APK until APK parsing is confirmed on-device.
