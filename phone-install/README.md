# BeatDrop phone install

Use this direct APK file from your phone if GitHub Releases or Actions artifacts download incorrectly.

## Direct raw APK link

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details for the current build:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `36348604a7c4db8b0d13b44de0ff1d3171af88d1a34b87cd5bff303b943a2520`
- Package: `com.beatdrop.app`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34

Changes in this build:

- Home is local/on-device music first.
- Local songs show a Spotify-style green checkmark.
- Home has a Songs list and an Explore your catalogue section.
- Search is online-only with Songs, Albums, Playlists, Artists filters.
- Search header/title/icon stay pinned while scrolling.
- Tapping an online song opens Now Playing instantly; tapping the currently playing song again does not restart it.
- Global online-loading spinner overlay removed.
- Album detail has an Apple Music-style blurred artwork background.
- Library is online/cloud only: first access prompts sign in, explains cloud/LAISACORP terms, then asks users to pick at least 3 artists for suggestions.
- Guest Home/Library no longer says Alex; signed-in users show their display name.

Database note: no database changes are required for this version. Artist picks are local-only. To sync them later, add a `user_favorite_artists` table or a JSON field in user settings/profiles.
