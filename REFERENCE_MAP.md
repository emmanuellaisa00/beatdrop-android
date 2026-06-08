# Reference Map — beatdrop-premium.html → Compose (exact)

Every value below is taken straight from the HTML's CSS. "≈" means a faithful
Compose equivalent of a web-only effect (e.g. backdrop blur, gradient-clipped text).

## Mini Player  (`.mini` → `ui/components/MiniPlayer.kt`)
| CSS | Value | Compose |
|---|---|---|
| `left/right` | 10px | `padding(start=10.dp, end=10.dp)` |
| `bottom` | 100px | `padding(bottom=100.dp)` |
| `height` | 66px | `.height(66.dp)` |
| `border-radius` | 20px | `RoundedCornerShape(20.dp)` |
| `padding` | 7 8 7 7 | `padding(start=7,end=8,top=7,bottom=7)` |
| bg | `rgba(20,12,18,0.88)` + top-left highlight | `MiniBg 0xE0140C12` + linear overlay `0x14FFFFFF→transparent@50%` |
| border | `rgba(255,255,255,0.12)` | `Color(0x1FFFFFFF)` |
| `.art` | 50×50, r12, gradient | `CoverArt(size=50.dp, corner=12.dp)` |
| `.t` | 14px / 700 | title style 14sp Bold |
| `.a` | 12px / 500 / `0.60` | sub 12sp `0x99FFFFFF` |
| `.cast`/`.play` | 38×38 circles | `IconCircle(38.dp)` |
| `.progress` | 2.5px, `0.08` track, accent fill | `height(2.5.dp)` `0x14FFFFFF` + `AccentBrush` |
| hide on Now Playing | `.hidden` | `AnimatedVisibility` |

## Now Playing  (`#screen-now`, `.np-*` → `ui/screens/NowPlayingScreen.kt`)
| CSS | Value | Compose |
|---|---|---|
| `.np-bg` | radial `#22405e` + linear `#0f2640→#0a1828→#040a12` | same vertical + radial brushes |
| `.np-bg::after` haze | `npHaze 9s alternate` | `infiniteRepeatable(tween(9000), Reverse)` ≈ |
| `.np-top` padding | 8 18 14 | `padding(start=18,end=18,top=8,bottom=14)` |
| chevron / more | 40×40 circle, `rgba(0,0,0,0.32)`, border `0.10` | `CircleButton(40.dp)` `0x52000000` border `0x1AFFFFFF` |
| chevron glyph | 20px | `size(20.dp)` |
| more glyph | 18px | `size(18.dp)` |
| `.album-name` | 12px/700, `+0.24em`, uppercase, `0.85` | 12sp Bold, `0.24f.em`, `uppercase()`, `0xD9FFFFFF` |
| `.np-cover` | margin 4/20, `aspect 1/1`, r20 | `padding(20,top=4)` `aspectRatio(1f)` `corner=20.dp` |
| cover border (inset) | `rgba(255,255,255,0.10)` | `border(1.dp, 0x1AFFFFFF)` |
| `.np-meta .nm` | 28px/900/`-0.030em` | 28sp Black `(-0.030f).em` |
| `.np-meta .ar` | 14px/600/`0.58` | 14sp SemiBold `0x94FFFFFF` |
| `.add` | 38×38 circle | `AddButton(38.dp)` |
| `.add.added` | bg `rgba(255,55,95,0.22)`, border `0.50` | `0x38FF375F` / `0x80FF375F` + check icon |
| `.np-progress .bar` | 5px, r3, `0.14` track | `height(5.dp)` `RoundedCornerShape(3.dp)` `0x24FFFFFF` |
| `.fill` | accent gradient, glow | `AccentBrush` |
| `.knob` | 16×16 white, ring `0.15` | `size(16.dp)` white + `border(3.dp,0x26FFFFFF)` |
| `.times` | 12px/600/`0.48` | 12sp SemiBold `0x7AFFFFFF` |
| `.np-controls` gap | 50px | `Arrangement.spacedBy(50.dp)` |
| `.ctrl` | 44×44, glyph 30 | `CtrlButton(44.dp)`, icon 34dp* |
| `.play-ring` | 74×74 circle, border `1.5px/0.32`, bg `0.06` | `PlayRing(74.dp)` `border(1.5.dp,0x52FFFFFF)` `0x0FFFFFFF` |
| play-ring glyph | 28px | `size(28.dp)` |
| `.np-bottom-actions` | padding 18/28 | `padding(start=28,end=28,top=18)` |
| `.ba` | 34×34, glyph 22, `0.80` | `BottomAction(34.dp)` icon 22dp `0xCCFFFFFF` |
| `.lyrics-drawer` | h84, radius `28 28 0 0`, bg `rgba(14,30,50,0.82)` | `height(84.dp)` top-corners 28dp `0xD10E1E32` |
| drawer label | 17px/800 | 17sp ExtraBold |

\* control glyph uses 34dp (Material `SkipPrevious/Next` are visually smaller than the
HTML's custom SVG at 30px; 34dp matches the rendered weight).

## Lyrics  (`#screen-lyrics`, `.lyrics-*` → `ui/screens/LyricsScreen.kt`)
| CSS | Value | Compose |
|---|---|---|
| bg | radial `#1e3d55` + linear `#0e2438→#091a2c→#040c18` | same brushes |
| top padding | `54px` (status) + 12/20/18 | `statusBarsPadding()` + `padding(start=20,end=20,top=12,bottom=18)` |
| `.mini-art` | 42×42, r8 | `CoverArt(size=42.dp, corner=8.dp)` |
| name | 15px/800 | 15sp ExtraBold |
| artist | 12px/600/`0.58` | 12sp SemiBold `0x94FFFFFF` |
| `.close` | 36×36 circle `rgba(0,0,0,0.42)` | `size(36.dp)` `0x6B000000` |
| `.line` | 26px/900/`-0.024em`, line 1.18, `0.28` | 26sp Black `(-0.024f).em`, `0x47FFFFFF` |
| `.line.active` | white, `scale(1.02)`, glow | white + `scale(1.02f)` |
| `.line.passed` | `0.15` | `0x26FFFFFF` |
| `.line` margin | 18px | `padding(vertical=18.dp)` |
| `.gap` dots | 3×8px, `lpulse 1.4s` staggered 0.18s | 3 dots, `tween(1400, delay=i*180)` Reverse |
| `.lyrics-transport` | l/r16, b22, h66, r33 | `padding(16,bottom=22)` `height(66.dp)` `RoundedCornerShape(33.dp)` |
| transport bg | `rgba(16,20,28,0.82)` | `0xD110141C` |
| `.play-mini` | 42×42 white, glyph 18 black | `size(42.dp)` white, icon 18dp black |
| transport `.bar` | 3px, `0.18` track, accent fill | `height(3.dp)` `0x2EFFFFFF` + `AccentBrush` |
| transport `.times` | 11px/600/`0.50` | 11sp SemiBold `0x80FFFFFF` |

## Library  (`#screen-library` → `ui/screens/LibraryScreen.kt`)
| CSS / HTML | Value | Compose |
|---|---|---|
| hero large title | "Your **Library**" | `Hero(titlePlain="Your", titleAccent="Library")` |
| filter pills | Playlists/Albums/Artists/Downloaded/Recently played | `FilterPills(...)` |
| quick grid | Liked/Downloads + albums | `QuickGrid(...)` |
| Recently played | carousel | `AlbumCarousel(...)` |
| From your library | track list + Shuffle | `TrackRow` list, `SectionHeader("From your library","Shuffle")` |
| initial screen | `setScreen('library')` | `var tab = Tab.Library` |

## Track row  (`.track` / `.track.playing` → `ui/components/TrackRow.kt`)
| CSS | Value | Compose |
|---|---|---|
| row padding | 9 12, radius 10 | `padding(h=12,v=9)`, `RoundedCornerShape(10.dp)` |
| `.track.playing` bg | `rgba(255,55,95,0.07)` | `Color(0x12FF375F)` |
| `.num` | 22px wide, 14/600/`0.45` | `width(22.dp)`, 14sp SemiBold `TextMuted` |
| `.art` | 44×44, r8 | `CoverArt(size=44.dp, corner=8.dp)` |
| `.t` | 15px/600 (`0.45`→white) | 15sp SemiBold |
| `.track.playing .t` | accent, 700 | `Accent`, Bold |
| `.a` | 12.5px/500/`0.48` | 12.5sp `0x7AFFFFFF` |
| `.dur` | 13px/500/`0.38` | 13sp `0x61FFFFFF` |
| `.more` | 18px | `size(18.dp)` `0x80FFFFFF` |
| `.eq` bars | 3px w, heights 40/90/60%, 750ms, delays -300/-100/-500 | `Equalizer()` `width(3.dp)`, `fillMaxHeight(peak)`, `tween(750)` Reverse, `StartOffset` |

## Scroll behavior — pinned frosted header  (`.compact` + scroll listener → `ui/components/CompactHeader.kt`)
This is the "name/search/icons become non-scrollable" effect you asked for.
| CSS / JS | Value | Compose |
|---|---|---|
| trigger | `el.scrollTop > 120` | `firstVisibleItemScrollOffset > 120.dp` (or index>0) via `derivedStateOf` |
| `.compact` height | 98px (54 status + 12 bottom) | `statusBarsPadding()` + `height(44.dp)` + `padding(bottom=12)` |
| reveal anim | opacity 0→1, `translateY(-6px)→0`, 220ms | `AnimatedVisibility(fadeIn + slideInVertically)` |
| frosted bg | `linear-gradient(rgba(8,6,10,0.88) 65%, transparent)` + blur 36 | vertical gradient `0xE008060A → transparent` (glass ≈ blur) |
| `.t` | 17px/800/`-0.016em` | `BeatType.TopBarTitle` |
| `.icons svg` | 22px, gap 22 | `size(22.dp)`, `spacedBy(22.dp)` |
| pins on Home/Library/Search/Add | per-screen compact bar | `CompactHeader` overlaid in each screen (Home="Home", Library="Library") |

## Search / Browse  (`#screen-search` → `ui/screens/SearchScreen.kt`)
| CSS / HTML | Value | Compose |
|---|---|---|
| `.search-field` | margin 20, h50, r25 | `padding(20)`, `height(50.dp)`, `RoundedCornerShape(25.dp)` |
| field bg / border | `rgba(255,255,255,0.08)` / `0.09` | `0x14FFFFFF` / `0x17FFFFFF` |
| field magnifier | 18px, `0.55` | `size(18.dp)` `0x8CFFFFFF` |
| placeholder | 14/500/`0.45`, "Artists, songs, podcasts and more" | 14sp `0x73FFFFFF`, same text |
| `.browse-grid` | 2 cols, gap 12, margin 20/24 | `Column`+`Row` `spacedBy(12.dp)`, `padding(h=20)` |
| `.browse-tile` | h100, r14, padding 14 | `height(100.dp)`, `RoundedCornerShape(14.dp)` |
| tile `::after` | radial highlight + bottom darken | two layered gradients |
| `.label` | 16px/900/`-0.022em`, shadow | 16sp Black `(-0.022f).em` white |
| `.deco` | 72×72, r10, `rotate(22deg)`, bottom-right `-8/-8`, bg `0.22` | `size(72.dp)` `rotate(22f)` `offset(8,8)` `0x38000000` |
| `bt-1..bt-8` | 2-stop linear-gradient 145deg | `BrowsePalette.BT1..BT8.brush()` (exact hex) |
| Top genres / Browse all | section headers | `SectionHeader(...)` |
| **search behavior** | (static mock in HTML) | **functional**: debounced live filter over real local tracks/albums (title/artist/album), with results vs. browse states |

## Album / Playlist detail  (`#screen-album` → `ui/screens/AlbumDetailScreen.kt`)
| CSS / HTML | Value | Compose |
|---|---|---|
| `.album-hero` padding | 110 20 24, centered | `padding(top=110,h=20,bottom=24)`, centered column |
| `.blur-bg` | h380, blur 70, scale 1.25, opacity .75 | `height(380.dp)`, `blur(70.dp)`, `scale(1.25f)`, `alpha(0.75f)` |
| `.cover` | 210×210, r16 | `CoverArt(size=210.dp, corner=16.dp)` |
| `.album-title` | 25/900/`-0.030em` | 25sp Black `(-0.030f).em` |
| `.album-meta` | 13/600/`0.65` "Artist · Album" | 13sp SemiBold `0xA6FFFFFF` (taps → artist) |
| `.album-stats` | 12/500/`0.42` "year · N songs · min" | 12sp `0x6BFFFFFF`, computed from real durations |
| `.album-actions` | gap 16, icons 42 circle | `Row spacedBy(16)`, `ActionIcon(42.dp)` |
| `.icon.liked` | accent stroke/fill | `Favorite` tinted `Accent`, toggles |
| `.play-big` | 58 circle, gradient `#FF375F→#d01e43` | `size(58.dp)` `PillActiveBrush` |
| sticky-back | frosts + reveals title past 120px | `StickyBackBar(frosted=...)` `derivedStateOf` |

## Sticky-back bar  (`.sticky-back` / `.frosted` → `ui/components/StickyBackBar.kt`)
| CSS | Value | Compose |
|---|---|---|
| height | 96 (54 status + 10) | `statusBarsPadding()` + `height(46)` + `padding(bottom=10)` |
| frosted bg | `linear(rgba(8,6,10,0.80)→transparent)` + blur 32 | animated `0xCC08060A→transparent` |
| `.center-title` | 16/800/`-0.016em`, opacity 0→1 | `animateFloatAsState` alpha |
| back / more | 36 circles | `CircleIcon(36.dp)` |

## Artist detail  (no HTML equivalent — Spotify-iOS recipe → `ui/screens/ArtistDetailScreen.kt`)
| Element | Spec | Compose |
|---|---|---|
| header | 360dp full-bleed image fading to bg | `SubcomposeAsyncImage` + vertical scrim |
| name | 40/900/`-0.038em` | 40sp Black |
| sub | "N songs in your library" | 13sp SemiBold |
| actions | outline Shuffle pill + 56dp gradient Play | `RoundedCornerShape(20)` border + `PillActiveBrush` |
| sections | Popular tracks + Albums carousel | `TrackRow` list + `AlbumCarousel` |

## Navigation architecture (Spotify-iOS → `ui/nav/Navigator.kt`)
- **One back stack per tab**; details push onto the *current* tab's stack.
- Switching tabs **preserves each tab's stack**; tapping the active tab **pops to root**.
- Detail pushes animate **slide-in-from-right**; pop slides back; tab switch fades.
- Mini player + dock **persist** above the nav host; system Back pops the stack
  (after closing Now Playing / Lyrics overlays).

## Add / Create  (`#screen-add` → `ui/screens/AddScreen.kt`)
| CSS / HTML | Value | Compose |
|---|---|---|
| hero | "Create" / "What's **new?**" | `Hero(greeting="Create", title="What's", accent="new?")` |
| `.add-list` | gap 10, margin 24/20 | `Column spacedBy(10)`, `padding(h=20)` |
| `.add-row` | padding 16, r16, bg 0.06 | `padding(16)`, `RoundedCornerShape(16)`, `0x0FFFFFFF` |
| `.add-row .icon` | 46 circle, accent gradient | `size(46.dp)` accent `Brush` (override per-row) |
| `.t` | 15/800/`-0.014em` | 15sp ExtraBold |
| `.d` | 12/500/`0.48` | 12sp `0x7AFFFFFF` |
| `.chev` | 22/300 "›" | `Text("›")` 22sp Light `0x59FFFFFF` |
| rows | Create / Blend / Scan / Paste link / Import | 5 `AddRow`s |
| Recent activity | "My Playlist #4 …" (mock) | **real** user-created playlists from `PlaylistStore` |
| Create a Playlist | `alert()` | **functional**: name dialog → persists a real playlist (SharedPreferences/JSON) |
| Import from Device | `alert()` | **functional**: launches the system audio picker (`ACTION_GET_CONTENT audio/*`) |

## Playlist detail  (extension — same recipe as album hero → `ui/screens/PlaylistDetailScreen.kt`)
| Element | Spec | Compose |
|---|---|---|
| hero | blurred backdrop + 210dp cover (album recipe) | reused `PlaylistHero` |
| meta | "You · Playlist", "N songs · X min" / "No songs yet" | computed from real tracks |
| actions | Add-tracks · Shuffle · 58dp gradient Play | `PlaylistActions` |
| empty state | "Let's build this playlist" + Add CTA | `EmptyPlaylist` |
| **add tracks** | (no HTML equivalent) | `ModalBottomSheet` multi-select over **real** device tracks; persists to `PlaylistStore`; already-added shown as checked/disabled |
| storage | — | `PlaylistStore` (SharedPreferences/JSON): create/add/remove/delete |
| nav | push `Destination.PlaylistDetail` | from Add → Recent activity row |

## Queue  (Spotify "Now playing / Next up" — `ui/screens/QueueScreen.kt`)
| Element | Spec | Compose / ExoPlayer |
|---|---|---|
| open | queue icon in Now Playing bottom row | `onOpenQueue` overlay |
| backdrop | Now-Playing teal gradient | same vertical gradient |
| Now playing | current item, accent title + dot | from `pb.currentIndex` |
| Next up | remaining queue, tap to jump | `controller.seekToDefaultPosition(i)` |
| reorder | up/down handles | `controller.moveMediaItem(from,to)` |
| remove | ✕ | `controller.removeMediaItem(i)` |
| shuffle | toggle | `shuffleModeEnabled` |
| repeat | off→all→one | `repeatMode` cycle |
| (also) play-next / add-to-queue | controller APIs ready | `addMediaItem(idx+1)` / `addMediaItem()` |

> All queue ops act on the **real ExoPlayer timeline** — state reflects back via the
> player's `onEvents` → `PlaybackState.queue/currentIndex`.

## Behaviors (from the `<script>` block)
| HTML JS | Compose |
|---|---|
| `openNowPlaying()` / `closeNowPlaying()` | `showNowPlaying` state + `AnimatedVisibility` slide |
| mini hidden on `now` | `MiniPlayer` `AnimatedVisibility(visible = !showNowPlaying ...)` via mini stays; NP overlays on top |
| `openLyrics()` / `closeLyrics()` | `showLyrics` state + fade overlay |
| play/pause mirrored mini↔NP↔lyrics | single source of truth: `PlaybackViewModel.state.isPlaying` |
| `+` add toggles check icon | `AddButton` local `added` state swaps Add↔Check |
| progress fill width | bound to **real** `positionMs/durationMs` from ExoPlayer |

## Web-only effects intentionally approximated
- **backdrop blur / saturate**: Compose has no first-class backdrop blur; reproduced
  with semi-opaque glass colors + subtle borders/overlays (visually equivalent on-device).
- **gradient-clipped text** (`.large-title .accent`, `-webkit-background-clip:text`):
  rendered as the solid accent color `#FF375F` (a `Brush` text style can be added later
  if you want the exact pink→`#ff7a94` gradient on the word).
- **box-shadow glows**: Compose elevation/shadow differs; key glows kept via color, not blur.
