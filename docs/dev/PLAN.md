# Project plan: 0.5 and beyond

Where we stand: 0.4.x is done and has run a week on a real stick with no issues. The refactor, the "feel" features, the city picker and
the 0.5 polish are in. This file is the plan; `CHANGELOG.md` says what shipped; `TESTING.md` says how to check it on a TV.

## What we learned from the field (research, Nov 2026)
- **Projectivy** (proprietary, freemium): categories and sort modes, video/Lottie/blur wallpapers, icon packs, parental control,
  plugins. Deep, but has a learning period. Paywall for icon packs and wallpapers.
- **ATV Launcher Pro** (paid): layout editor on the Menu key, many row styles, widgets inside rows, modular status bar, input switching,
  15 clock styles, VPN indicator, Home-button redirect.
- **tvOS 26**: subtle Liquid Glass, Control Center with a sleep timer and a "movie night" scene, profile picker on wake, choosable screen
  saver clips.
- **Google TV**: ads and a Shorts row on Home, AI ambient art. The thing to be the opposite of.
- **FLauncher / Sideload Launcher**: light and private, but plain. **Primal**: pretty, needs a strong TV.

## Lessons from couch-tv and Couchy (checked Nov 2026)
**couch-tv** (Seagey, MIT) is a different kind of launcher: a web-tech home screen, a phone remote and a computer-side bridge, set up with
the help of an AI agent, for Google TV, Samsung, LG, Roku and Fire TV, with Home Assistant, scenes and an assistant. Not our lane (we are a
small native launcher that needs no computer after setup), but it taught us:
- **Safety**: it found that switching off Google TV's home and setup apps can leave some TVs looping at every restart, which our scripts did
  too. Fixed in 0.6.1 (scripts leave them on, `--disable-stock` is expert-only and never touches the setup app, older changes are undone,
  a crash guard hands Home to the TV's own home after two crashes in five minutes, and the setup page explains recovery).
- **Agent-friendly repo**: `AGENTS.md` with the safety rules (done). **Privacy scan** for forks and commits: `tools/privacy-scan.sh` (done).
- **Depth clock**: a big clock that sits *behind* a foreground silhouette (mountains, skyline). Fits the live-sky wallpaper (0.8) and needs
  no art files if the ridges are drawn procedurally.
- **OLED black look**: pure black background with dim glass for OLED TVs (0.7). **Glass strength** steps plus an honest "no blur" fallback
  (we have the Frosted glass switch; add strengths in 0.7).
- **Quick buttons** worth having in the quick panel (0.9): sleep timer, screen saver, input switcher with names, and soundscapes made by a
  small noise generator (rain, brown noise) rather than audio files.
- **Languages** are a feature, not a chore: couch-tv ships several. Translations move up to 0.7.
- **Opt-in update check** from GitHub releases with a one-tap install (0.9, as a third optional network feature, off by default).
- **Home-button redirect**: couch-tv uses an accessibility service to get Home back. For us it is now the *non-destructive* way to make Home
  open shangOS on Google TVs, since we no longer switch their launcher off. Moves up to 0.7, opt-in and clearly explained.

**Couchy Launcher** (conreo, GPL-3, on F-Droid): carousel, grid and dock layouts, opt-in aerial video wallpapers, a VPN icon that opens the VPN
app, 17 languages, distributed through F-Droid. Takeaways: a tap on our VPN lock opens the VPN app or the system VPN page (0.7); submit
shangOS to **IzzyOnDroid** (APKs from GitHub releases) and later F-Droid (0.9 to 1.0); curated arrangements stay at 3, not many.

Not adopting: web-tech UI, a required computer bridge, AI control of the TV, other TV brands, smart-home control.

## Our lane
- Open source, no paywall, no ads, no accounts, no analytics.
- Smooth on a 1.4 GB stick; richer on better hardware (Light / Balanced / Quality tiers).
- Premium look from cheap tricks (pre-rendered, cached, cross-faded), never from per-frame blur or video.
- "The screen belongs to this room and tells the truth about it": real sun, real weather, real progress, nothing sold.
- Few good choices rather than endless toggles.

## Rules for every feature
1. Must run on the Light tier, or be off there. 2. No new permission unless it is the point (and then documented and audited).
3. Nothing leaves the TV except the two optional features. 4. One class, one job; `MainActivity` stays under 800 lines (lint).
5. Pure logic gets unit tests. 6. Every claim in the UI or docs is true. 7. Each release has a checklist in `TESTING.md`.

## Animation decision: no Lottie library
- `lottie-android` needs AppCompat (AndroidX), which this project does not use, and adds roughly 300 KB to 1 MB plus many methods.
- We use native animation instead: property animators, springs, `AnimatedVectorDrawable`, and small procedural drawables (the shimmer, the
  status icons, the backdrop). All are free of dependencies and tiny.
- Revisit only for **Lottie wallpapers** (0.8): then write a small renderer for a restricted subset (shape layers, transforms, trim paths,
  fills and strokes), not the full library. Decide after the live-sky wallpaper, which may make it unnecessary.

## 0.5 Polish (done in this build)
- Frosted glass: the wallpaper blurred once into a 96-pixel picture; bars, cards and menus draw their part of it. Setting: Frosted glass;
  off on Light.
- Hero backdrop: the resume programme's artwork, faded into the wallpaper behind the hero.
- Calm mode: after 20 seconds the tab bar and clock fade back; any key brings them back. Setting: Calm mode.
- Skeleton shimmer instead of "Finding your apps...".
- Rise-in: greeting, widgets and tiles rise into place on start.
- Tile shadow tinted with the app's colour (the focused card glows faintly in its own colour).
- Status icons: a lock while a VPN is on, an amber crossed Wi-Fi when offline; nothing otherwise.
- Settings: the screen saver rows moved to `ScreenSaverSettings` to keep `SettingsScreen` under its limit.

## 0.6 Rows and layout (started)
Done in 0.6.0:
- Settings in pages (a first page of five entries; groups are pages; `SettingsPages` builds the rows).
- Apps sort modes (manual, A to Z, recently used) and a hidden-apps manager.
- The chosen city also sets sunrise and sunset.

Still to do:
- Rows you choose: Favourites, Recents, Continue watching (poster cards from Watch Next), Inputs.
- Two or three curated arrangements (Spotlight, Dock, Posters), not ten.
- Menu key opens a live edit mode on Home itself (the guide promised a Menu key once).
- Folders, built on grab-to-move. Screen breaks. Optional 2-row Home plus a hint that the Apps tab has the rest.
- Rename an app. Hero button launches the resumable programme through its deep link.
- Replace the hardcoded pinned-app list with a rule based on recents.

## 0.6.4 (done)
- Website port (Astra), Inter bundled, status capsule with the clock, Sun and Moon and System widgets.
- Still to do from the agreed list: Clock pack (analogue, flip, words), the weather refinement, the Large and Extra-large size classes
  (more room on bigger TVs), and replacing the Preview size row with them.

## 0.6.1 Safety (done)
- Scripts never switch off the TV's own home or setup apps by default (`--disable-stock` / `-DisableStock`, asks first, never the setup
  app); they switch back on anything an older version switched off; `status` warns about it.
- Crash guard: two crashes within five minutes make shangOS hand Home to the TV's own home, and try again later.
- The setup page has "If your TV loops after a restart"; `AGENTS.md`; `tools/privacy-scan.sh`.

## 0.7 Widgets pack (no new permissions)
- Clocks in 4 or 5 styles (analogue, flip, words, minimal).
- Sun and Moon: sunrise, sunset, golden hour, moon phase, computed on the TV from the chosen city.
- Countdowns and events, set on the TV or pushed over adb.
- World clocks from the built-in city list (nearest time zone).
- Kitchen timer and stopwatch made for a d-pad.
- Photo frame from private storage (same door as the photo wallpaper).
- Weather detail: hourly strip and rain in the next hour (same host as now).
- Plus from the research: opt-in Home-button redirect (accessibility service), language picker and the first translations, OLED black look and
  glass strengths, VPN lock opens the VPN app.
- System glance: Wi-Fi strength, free storage and memory.
- Now Playing as a full-bleed album-art card.

## 0.8 Ambient
- **Live sky** wallpaper: a gradient drawn from the real sun position at the chosen city, with stars at night; no image assets.
- Photo slideshow screen saver from pushed pictures, with a slow pan.
- Album-art screen saver while music plays.
- Choose which scenes the screen saver shows.
- Optional looping video wallpaper from a file pushed over adb (not bundled).
- Decide here about Lottie wallpapers (see above).

## 0.9 Control and trust
- Quick panel (Control Center style): profiles, input switching, an app-level sleep timer, "Movie night" (dim the room light, hide chrome).
- Profiles with their own order, hidden apps and wallpaper; a kids profile; optional PIN on chosen apps.
- Icon pack support (the standard icon-pack format).
- Privacy receipt: a screen listing every network request the app made (host, time, count). `Net` is the only door out, so it is cheap.
- (Moved up to 0.7: the opt-in Home-button redirect.)
- Backup and restore of all settings to one local file (`shangos.sh backup`).

## Before 1.0
- Right-to-left check, TalkBack pass, text-size setting, high-contrast mode (solid panels). (Strings into resources and the first translations
  move up to 0.7, with the language picker.)
- Baselines per release on a 1.5 GB TV: cold start, per-launch time, idle CPU and memory, APK size.
- Reproducible-build notes, threat model, a test for every privacy claim on the website.
- Two weeks of daily use on at least three TVs, all checklists green, audit green, website matching the app.

## Parked or rejected
- Rejected: ads, sponsored rows, Shorts or shopping rows, analytics, AI-generated art from a service, ten fixed layouts and seven focus effects.
- Parked: a phone companion for typing and photos (needs a local server and network permission), voice search, notifications row,
  game-controller navigation, Cast or Matter controls.
