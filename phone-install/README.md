# BeatDrop phone install

Use this direct APK file from your phone if GitHub Releases or Actions artifacts download incorrectly.

## Direct raw APK link

https://raw.githubusercontent.com/emmanuellaisa00/beatdrop-android/main/phone-install/BeatDrop.apk

Expected details for the current build:

- File name: `BeatDrop.apk`
- Size: about `24 MB`
- SHA256: `994fe5b208f565a4cf2fbf1624656585c3cb487e3f51ec75f2b9d604c343837d`
- Package: `com.beatdrop.app`
- minSdk: 23
- targetSdk: 33
- compileSdk: 34

Changes in this build:

- Cloud client now matches the new backend schema: `external_items`, `library_items`, `liked_items`, `playlist_items`, `recently_played`, `listening_history`, `search_history`, `user_favorite_artists`, and `followed_artists`.
- Old cloud table calls (`songs`, `liked_songs`, `playlist_songs`) have been removed from the sync bridge.
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

Dashboard reminder: add `beatdrop://auth` under Supabase Authentication → URL Configuration → Redirect URLs for password reset/deep-link auth callbacks.
