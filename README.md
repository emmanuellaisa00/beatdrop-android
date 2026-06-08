# BeatDrop (Android · Kotlin · Jetpack Compose · Media3 ExoPlayer)

A native Android music player that reproduces the `beatdrop-premium.html` design
in Jetpack Compose, backed by a **real ExoPlayer** (Media3) — **no mock data**.

## Why Kotlin native (not React Native)
You asked for "real ExoPlayer." ExoPlayer *is* Android's native player (Media3),
so going native gives first-class playback, background audio, a system media
notification and lock-screen controls with the least friction — and Compose
renders the glassy gradients/blur of this design beautifully. (If you ever need
iOS too, that's the case for React Native — tell me and we can revisit.)

## Data strategy
- **Local:** reads the device's real music library via `MediaStore`
  (`LocalMusicSource`) — genuine tracks, albums and embedded artwork.
- **Online (live):** YouTube **Innertube** engine (`data/online/`), the same
  approach as github.com/emmanuellaisa00/beatdroppremium — **no API key**:
    - `YoutubeService.searchSongs/Albums/Playlists` → YT-Music
      `music.youtube.com/youtubei/v1/search` (WEB_REMIX, category-filter params).
    - `playlistTracks` → `/next` (watch queue) for album/playlist tracklists.
    - `getStream(videoId)` → `/player` multi-client chain
      (ANDROID_TESTSUITE → ANDROID_VR → ANDROID → IOS) returning plain audio URLs,
      90-min cache. Resolved lazily right before playback.
  Online results map to the same `Track`/`Album` models (`MediaSource.ONLINE`,
  `onlineId`), so every existing screen renders them unchanged.

## Architecture
```
data/
  model/Models.kt          Track, Album, Shelf, QuickItem (+ MediaSource)
  local/LocalMusicSource    MediaStore queries (real device audio)
  repository/MusicRepository Single data entry point (local now, online later)
player/
  PlaybackService           Media3 MediaSessionService (real ExoPlayer)
  PlayerController           MediaController -> StateFlow for Compose
ui/
  theme/                    Colors, type, cover gradients ported 1:1 from CSS
  components/               CoverArt, Hero, Sections, ScreenBackground
  nav/Dock.kt               Glass bottom dock
  screens/HomeScreen        Milestone 1 (built)
  App.kt                    Shell + tab switching
```

## Build & run
1. Open the `BeatDrop/` folder in **Android Studio** (Ladybug or newer).
2. Let it sync (downloads Gradle 8.9, AGP 8.5, Compose BOM, Media3 1.4.1).
3. Run on a device/emulator with some audio files present.
4. Grant the audio permission when prompted.

> Built and reviewed in a headless sandbox (no emulator), so it hasn't been
> run on a device yet — Android Studio's first sync will fetch dependencies.

## Status
**Milestone 1**
- [x] Project scaffold, Gradle, manifest, theme tokens (exact palette/type)
- [x] Real data layer (MediaStore) + repository abstraction
- [x] Real ExoPlayer service + controller (StateFlow)
- [x] **Home screen** — hero, filter pills, quick grid, album carousels
- [x] Glass bottom dock (Home/Search/Library/Add)

**Milestone 2 (this build)**
- [x] **PlaybackViewModel** — shared real-ExoPlayer state + position polling
- [x] **Mini Player** — appears when audio is playing; tap to expand
- [x] **Now Playing** — cover, meta, draggable progress, transport, lyrics drawer
- [x] **Lyrics** — full-screen overlay with transport pill
- [x] Wired so tapping an album on Home starts real playback → mini → now playing

**Milestone 3 (this build)**
- [x] **Library screen** — hero, filter pills, quick grid, recently-played carousel, track list
- [x] **TrackRow** with animated playing equalizer (accent bars)
- [x] **Pinned frosted compact header** — hero title scrolls away, screen name +
      search/add icons stay fixed at top once scrolled past ~120px (HTML `.compact`
      scroll behavior). Applied to Home and Library.
- [x] Tap a track to start real ExoPlayer playback; playing row shows the equalizer
- [x] App opens on the Library tab (matches HTML init)

**Milestone 4 (this build)**
- [x] **Search / Browse screen** — search field, Top genres + Browse all tile grids
- [x] **BrowseGrid / BrowseTile** with bt-1..bt-8 gradients + rotated deco glyphs
- [x] **Functional search** — debounced live filtering over the real local library
      (songs + albums), with results / browse states (HTML's was static)
- [x] Pinned frosted "Search" header on scroll

**Milestone 5 (this build)**
- [x] **Album / Playlist detail** — blurred backdrop, 210dp cover, title/meta/stats,
      action row (like/download/share/shuffle + 58dp gradient play), full track list
- [x] **Artist detail** (Spotify-iOS recipe; no HTML original) — full-bleed header,
      name, Shuffle/Play, Popular tracks, Albums carousel
- [x] **StickyBackBar** — frosts + reveals title on scroll (HTML `.sticky-back`)
- [x] **Spotify-iOS navigation** (`Navigator`) — per-tab back stacks, push/pop with
      slide animations, persistent mini player + dock, active-tab-taps-to-root
- [x] Data layer extended with **Artist** model + MediaStore artist grouping
- [x] Album meta line taps through to the artist screen

**Milestone 6 (this build)**
- [x] **Add / Create screen** — hero, action list (Create/Blend/Scan/Paste/Import),
      Recent activity
- [x] **AddRow** component ported 1:1 (46dp gradient icon, title/desc, chevron)
- [x] **Functional Create-a-Playlist** — name dialog persists a real playlist on device
      (`PlaylistStore`, SharedPreferences/JSON)
- [x] **Functional Import from Device** — launches the system audio picker
- [x] All four main tabs now built (Home / Search / Library / Create) + detail screens

**Milestone 7 (this build)**
- [x] **Playlist detail** — open a created playlist (album-hero recipe), empty state,
      Play/Shuffle, real track list
- [x] **Add-tracks flow** — modal bottom sheet, multi-select over real device tracks,
      persisted to `PlaylistStore`; already-added tracks shown as checked
- [x] `PlaylistStore` extended (add/remove/delete) + repo `playlistAsAlbum`
- [x] Wired Add → Recent activity → Playlist detail, via `Destination.PlaylistDetail`

**Milestone 8 (this build)**
- [x] **Queue screen** — Now playing / Next up, backed by the **real ExoPlayer queue**
- [x] Tap-to-jump, reorder (up/down), remove, shuffle + repeat (off/all/one)
- [x] Player layer exposes queue/currentIndex + playNext/addToQueue/move/remove
- [x] Opened from the Queue icon in Now Playing

**Milestone 9 (this build) — Online catalogue (YouTube Innertube, no API key)**
- [x] `data/online/` — `YoutubeService` (search songs/albums/playlists, playlist
      tracklists, multi-client stream resolver) + models + mappers
- [x] OkHttp dependency; ExoPlayer wired with an HTTP data source for streams
- [x] Search screen: **On device ⇄ Online** toggle, live results + loading state,
      online Songs/Albums/Playlists; tap a song to stream, open album/playlist to
      fetch its tracks
- [x] `PlaybackViewModel` resolves online stream URLs lazily (fast first track,
      rest resolved in background) with a buffering indicator
- [x] `Track.onlineId/streamUserAgent`, `MediaSource.ONLINE` end to end

**Milestone 10 (this build) — Cipher/WebView fallback + loading states**
- [x] `YoutubeCipher` (Mozilla Rhino) — downloads base.js, extracts the `sig` +
      `nsig` transforms and runs them, so **ciphered** formats now resolve
      (yt-dlp / NewPipe method)
- [x] `YoutubeWebViewExtractor` — headless WebView reads
      `ytInitialPlayerResponse.streamingData` as a BotGuard-immune last resort
- [x] `getStream()` chain: warm cipher → Innertube clients (cipher-aware) → WebView
- [x] Rhino dep + proguard keep rules; extractor inits in `BeatDropApp`
- [x] **Skeleton loading** (shimmer) on Home, Library, Album/Artist/Playlist detail,
      and online Search results; **spinner** kept only for the indeterminate online
      stream-buffering moment

**Milestone 11 (this build) — Liked Songs + global ⋯ menu**
- [x] `LikesStore` (persists full Track payload) + app-wide `LikesViewModel`
- [x] **Liked Songs** screen (heart hero, play/shuffle, real list) + Home/Library tile
- [x] **Global track ⋯ context sheet** (`LocalTrackMenu`): like, play next, add to
      queue, add to playlist, go to artist/album, share — works from every TrackRow
- [x] **Add-to-playlist sheet** (pick or create a playlist, persists)
- [x] Now Playing heart wired to real likes; system share intent

**Milestone 12 (this build) — Downloads + offline**
- [x] `DownloadManager` (long-lived scope, OkHttp streaming, per-track progress
      StateFlow, persistent index) — resolves the stream then saves to files dir
- [x] **Per-row download button** on online tracks: ↓ / queued / progress ring % / ✓
- [x] Completed downloads become offline LOCAL tracks, merged into the Library
      and surviving restarts

**Milestone 13 (this build) — Dedicated Downloads view**
- [x] **Downloads screen** — offline songs, storage usage, play/shuffle, per-row delete
- [x] `DownloadManager.delete()` (removes file + index) and `usageBytes()`
- [x] `Destination.Downloads` wired from the Home/Library "Downloads" tile

**Milestone 14 (this build) — Synced lyrics (LRCLIB)**
- [x] `LrcLibProvider` (lrclib.net, no API key) + `LrcParser` — exact match then
      search fallbacks, prefers synced, converts plain→timed; in-memory cache
- [x] Lyrics screen now shows **real synced lyrics**: active line highlights +
      auto-scrolls to playback position; tap a line to **seek**; loading spinner
      and honest "No synced lyrics" state

**Milestone 15 (this build) — Profile + Settings + Equalizer + Sleep timer**
- [x] `AudioFx` — real system `Equalizer` + `BassBoost` attached to ExoPlayer's
      audio session; presets, per-band faders, persisted + re-applied on attach
- [x] `SleepTimer` — countdown that pauses the player; live remaining time
- [x] `SettingsStore` — gapless / normalize / data-saver / stream+download quality
- [x] **Profile** screen (avatar tap in any Hero) → Liked, Downloads, Equalizer,
      Sleep timer, Settings
- [x] **Settings** + **Equalizer** screens; `MODIFY_AUDIO_SETTINGS` permission

**Milestone 16 (this build) — Foreground downloads + sidecar .lrc**
- [x] `DownloadService` — foreground service with an aggregate progress
      notification; started on enqueue, self-stops when downloads finish, so
      downloads survive backgrounding/process pressure
- [x] `POST_NOTIFICATIONS` runtime request (API 33+) + `dataSync` FGS type
- [x] **Sidecar `.lrc`** — local tracks with a matching `.lrc` file use those
      synced lyrics before falling back to LRCLIB (`Track.filePath` from MediaStore)

**Milestone 17 (this build) — Crossfade**
- [x] `CrossfadeEngine` — 250ms ticker ramps ExoPlayer volume down near a track's
      end and back up on the next item (volume-bridge crossfade, no 2nd player)
- [x] Crossfade duration setting (0–12s) in Settings; `CrossfadeSettings` shared
      process-wide and hydrated on app start; bound/released in `PlaybackService`

**Milestone 18 (this build) — Cloud backend Phase 0 (Supabase, vendored)**
- [x] Vendored the Supabase integration kit into `data/cloud/` (client, auth,
      models, repositories, realtime, storage, view-models), repackaged to
      `com.beatdrop.app.data.cloud`
- [x] Added Supabase BOM 3.0.3 + Ktor + kotlinx-serialization plugin/deps
- [x] Fixed SDK-3.x API mismatches (update(buildJsonObject), notif count,
      tolerant SessionStatus when, json imports); set deep-link scheme `beatdrop://auth`
- [x] Not user-facing yet — cloud is **opt-in** (guest by default); auth/sync land next

**Milestone 19 (this build) — Cloud Phase 1 (optional Auth + Profile)**
- [x] `AuthScreen` — sign in / create account / reset password (Supabase Email),
      loading + error states; opens only when the user opts in
- [x] Profile is auth-aware: **Guest** with a "Sign in to sync" CTA by default;
      shows real name/email + Sign out when authenticated
- [x] `AuthManager` held at app level; auth overlay auto-dismisses on sign-in;
      app stays 100% usable signed-out

**Milestone 20 (this build) — Cloud Phase 2 (local-first sync)**
- [x] `CloudSync` — maps BeatDrop tracks ↔ cloud `songs` via `external_source_id`
      (`ensureSongId` resolves or lazily creates the catalog row)
- [x] **Likes**: toggle writes locally first, mirrors to Supabase; on sign-in,
      cloud likes merge into local (union) and local-only likes push up
- [x] **History / recently-played**: each play best-effort recorded to the cloud
- [x] Everything wrapped best-effort so cloud/RLS failures never break local UX;
      song-id cache cleared on sign-out

**Milestone 21 (this build) — Cloud Phase 3 (social/realtime)**
- [x] **Notifications** screen + inbox, realtime inserts via `NotificationsRealtimeManager`,
      unread badge on the Profile bell, mark-all-read; live only when signed in
- [x] Realtime subscribe/unsubscribe tied to auth state
- [~] Avatar/cover **uploads** + **follow artists**: `StorageManager` + repos are
      vendored and ready, but need a remote artist/user catalog + image-picker
      wiring (deferred — local artists are name-keyed, no cloud UUID yet)

Cloud integration is functional end-to-end for the personal-data path (auth →
likes/history sync → notifications). Remaining social bits are scoped for later.
- [ ] Phase 3: public playlists, profiles, followed artists, realtime notifications, uploads
- [ ] Lock-screen download controls, lyrics romanization, widget

See `REFERENCE_MAP.md` for the exact CSS→Compose value mapping.
```
