# Loading states — skeletons vs spinners

Principle:
- **Skeleton (shimmer placeholder)** → when we know the *shape* of the content
  that's coming (lists, grids, hero+tracks). Feels faster, no layout jump.
- **Spinner (CircularProgressIndicator)** → short, *indeterminate* actions where
  there's no meaningful layout to preview yet.

Reusable skeletons live in `ui/components/Skeleton.kt`:
`SkeletonBox`, `SkeletonCarousel`, `SkeletonTrackList`, `SkeletonHomeContent`,
`SkeletonDetailContent`.

| Screen / action | State source | Treatment | Status |
|---|---|---|---|
| **Home** initial load | `HomeViewModel.loading` (MediaStore scan) | `SkeletonHomeContent` (hero + quick grid + 2 carousels) | ✅ |
| **Library** initial load | `LibraryViewModel.loading` | `SkeletonHomeContent` | ✅ |
| **Album detail** fetch | `album == null` | `SkeletonDetailContent` (cover + meta + tracks) | ✅ |
| **Artist detail** fetch | `artist == null` | `SkeletonDetailContent` | ✅ |
| **Playlist detail** fetch | `album == null` | `SkeletonDetailContent` | ✅ |
| **Search — online** query | `SearchUiState.loading` | Skeleton carousel + track list | ✅ |
| **Search — local** query | (instant filter) | none needed | ✅ |
| **Online stream buffering** | `PlaybackViewModel.resolving` | center **spinner** (indeterminate; URL resolve + base.js) | ✅ |
| **Image loads** (covers) | Coil | gradient placeholder via `CoverArt` (not a spinner) | ✅ |
| Mini player / Now Playing | ExoPlayer state | progress bar; no separate loader | ✅ |

| **Per-row download** (online tracks) | `DownloadManager.jobs[trackId]` | inline button: ↓ idle / pulsing queued / **progress ring + %** / ✓ done | ✅ |
| **Lyrics fetch** (LRCLIB) | `loading` in LyricsScreen | center **spinner**, then lines or "No synced lyrics" | ✅ |
| **Equalizer** (no session yet) | `AudioFx.state.available` | informative message until playback starts | ✅ |
| **Background downloads** | `DownloadService` | system **progress notification** (n songs · %) | ✅ |

## Not yet added (future)
- **Pull-to-refresh** spinner on Home/Library if we add manual refresh.
- **Pagination footer** spinner if online search gains "load more".
- **Search suggestions** typing affordance (debounce already hides flicker).
