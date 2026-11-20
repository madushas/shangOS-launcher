# Changelog

## 0.6.7: Project cleanup and R8 builds
- **R8 shrinking for release and offline builds.** `build.sh` runs R8 with `proguard-rules.pro` (shrink and optimise, no renaming, so crash reports
  stay readable). The aapt2 link step supplies the keep rules for manifest components. Debug still uses d8; `SHRINK=0` skips R8; a build-tools
  without R8 falls back to d8 with a note. `mapping.txt` is written to the build folder.
- **`androidx.annotation` as a compile-only jar in `libs/`** (see `libs/README.md`). `@WorkerThread`, `@MainThread`, `@NonNull` and `@Nullable`
  mark the blocking and main-thread-only calls (`Cities.of`, `WatchNext.query`, `PhotoStore.importFrom`, `WallpaperArt.compose`, `Wallpaper.load`,
  `PageMotion`). Nothing is packaged in the APK.
- **Fixed: Settings → Home could crash when an app is hidden.** The Hidden apps row was given the app list before it was created. The list is now built first.
- **Fixed: the parallax and tile depth assumed six tiles per row.** The default layout has five (Comfortable) and Cozy has four, so the wallpaper
  and the tiles leaned to one side. Both follow the layout size now (`Metrics.rowPosition`).
- **Fixed: `test/run.sh` did not compile.** `Prefs` named `Wallpaper`, which the unit tests cannot include. The schema 2 migration now keeps its own
  constant (a migration must not follow a list that grows). Added a test for it.
- **Fixed: the Home widgets row listed only four widgets.** It now also names Sun and Moon and System.
- **Size budget met again:** `MainActivity` was 829 lines against 800 and `Wallpaper` 701 against 700. The page entrance is now `PageMotion`, the
  picture fitting is `WallpaperArt`, and the photo copy is `PhotoStore.importFrom` (a picture over the size limit is refused and its partial copy deleted). `MAIN_MAX` is 790.
- **Removed dead code:** `Theme.ON`, `WidgetBoard.pageCount`, `Tint.isLight` (and its test), four unused locals in `SettingsPages`, an unused local in
  `Wallpaper.load`, an orphan Javadoc, and the `dalvik-exchange` fallback in `build.sh` (d8 has shipped with build-tools since 28).
- **Tidied:** the manifest's checked-in version now matches `VERSION`, the Now Playing comment sits above its service, `SettingsPages` uses `Prefs`
  key constants, `AppRepo.TRIM_BACKGROUND` names the trim level that was a bare `40`, `Devices` now has a JUnit test in `test/` that CI runs
  (it was a stand-alone program in `tools/`).
- Tests: 93. VERSION 0.6.7 (versionCode 21).

## 0.6.6: Benchmarks state the performance choice they measured
- **`tools/bench.sh` needs no environment variables.** It makes each launcher the default Home and compiles it first (`SET_HOME=1`,
  `COMPILE=speed`, now the script's own defaults), and it puts the TV's Home back when it finishes.
- **`--perf auto|light|quality`** says which Performance you set in shangOS's Settings (Motion and sound, Performance) before the run. The
  script does not change it: a release build cannot be changed over adb. The script asks you to set it and waits for Enter. The report
  records the setting and the tier it gives on this TV.
- **Auto is predicted, not read.** On a TV with 1.4 GB of RAM and a 32-bit system, Auto gives the Light tier, where the living wallpaper and
  frosted glass are off. The script works out the tier from the TV's RAM, cores, bitness and low-RAM flag, with the rules the app uses.
- **Why this matters:** a run on Balanced or Quality measures the living effects, so it can show a higher idle CPU. Before this change the
  report did not say which setting ran.
- Tests: 93. VERSION 0.6.6 (versionCode 20).

## 0.6.5: An interactive wizard, and build checks that catch what slipped
- **The shangos.sh wizard is a real menu now.** Up and Down move, Enter chooses, number keys jump, and q goes to the last item (usually Quit).
  Yes/no questions toggle with Left and Right (or type y or n), and Enter confirms the highlighted answer. The cursor is restored when the
  wizard exits, however it exits. Without a terminal (a pipe, TERM=dumb, or SHANGOS_PLAIN=1) every question falls back to typed numbers and y/n.
- The shared TUI code lives in `tools/tui.sh` and is copied into `shangos.sh` by `tools/inline-tui.py`; lint fails if the copy drifts.
- `tools/tests/tui_test.py` drives the menus in a real pseudo-terminal with arrow keys: 12 checks.
- `tools/check-strings.py` applies the aapt rules to the string resources (escaped apostrophes and quotes, escaped ampersands). It would have
  caught the strings.xml error before a build. Lint runs it.
- `docs/setup.sh` already had arrow-key menus; its menu was checked in the same pseudo-terminal harness.
- VERSION 0.6.5 (versionCode 19).


## 0.6.4 (fourth build): the power button opens the TV's power dialog
- The power button opens the TV's own power dialog (restart, power off and whatever the TV offers), which asks for its own confirmation.
  shangOS does not restart or switch off the TV itself any more.
- That needs an optional accessibility service, **shangOS power button**, turned on in Settings, Accessibility. It is off until you turn it
  on. It has no access to the screen's content and handles no events; the privacy page lists it. Until it is on, the button explains how.
- `./shangos.sh restart` and `shutdown` are removed, for the same reason.
- Tests: 93. VERSION 0.6.4 (versionCode 18).

## 0.6.4 (third build): status bar, focus colour, power
- **Status bar is smaller than the tabs**: 36 units against the tabs' 40, with a 16-unit clock.
- **The new cards no longer turn into a white box on focus.** Sun and Moon and System now use the same translucent highlight as the Weather
  card, so their text stays readable.
- **Power button** at the right end of the status bar. OK opens Restart or Shut down, then asks again before it acts. Android does not let a
  normal app restart or power off a TV (the REBOOT permission is for system apps only), so on most TVs the launcher says so and points to the
  remote's power button and to `./shangos.sh restart` or `./shangos.sh shutdown`, which run over adb after a confirmation. On a TV where the
  launcher is a privileged system app the buttons act directly. Nothing is changed unless the TV accepts.
- Focus moves Settings tab, then Right to the power button; Left returns to the Settings tab.
- Tests: 93.

## 0.6.4 (second build): fixes from the first TV test
- **Status capsule** sizes to the icons and the time it holds, and sits on the right, clear of the tab bar. Before it was a fixed width and
  overlapped the tabs.
- **Pages fill the row.** Sun and Moon and System share any spare width (up to 560 each), and the group is centred. A card that is off (Weather
  when offline, say) no longer leaves a gap: the cards left take the space.
- **Focus moves across all pages.** Sun and Moon and System are focusable as whole cards, so a page with no buttons can still be left with
  Left or Right.
- Tests: 93 (fitting rule, unit tested as `CardFit`).

## 0.6.4: Website port, a bundled typeface, a status capsule, two new widgets
- **Website**: the new design (Astra) is ported into `tools/build-site.py`: one header with a menu, the theme choice, the Download button, the
  footer, a strict Content-Security-Policy (`docs/_headers`, with the hash of the one inline script), and the new home page. The version on
  the site comes from `VERSION`. The setup, guide, privacy and licence pages keep their content and now use the new look.
- **Typeface**: Inter (Regular and Medium, Latin, about 83 KB each) is bundled, so every TV draws the same letters. It is credited in Settings,
  Licences and `NOTICE.md`. If the font cannot be read, the system face is used.
- **Status capsule**: the clock and its status icons (network, VPN, muted) share one frosted panel at the top right, the height of the tab
  bar. Calm mode fades the panel as a whole.
- **Sun and Moon** widget: where the sun is between today's sunrise and sunset (the chosen city, else the time zone's), the two times, and
  the moon's phase with its lit part. Computed on the TV; no permission, no network.
- **System** widget: the network (Wi-Fi with its bars, Ethernet, or offline), free storage and free memory. Reads the system's own numbers, no
  new permission; refreshes every 30 seconds while Home is shown.
- Both new widgets are on by default and can be turned off in the widget editor.
- Under the hood: `Moon` (the phase, unit tested), `SunMoonWidget`, `GlanceWidget`. Tests: 89.
- **Benchmark names the launcher it measured.** The TV's own launcher is now labelled by what it is (Google TV Home, Android TV Home, or the
  TV's own home) instead of a generic "stock". The site flags any shangOS or Luma TV measurement that is behind the latest release. Luma TV's
  latest known release is 2.5; the page's current numbers are Luma 2.4, so they need re-measuring on a TV.
- Not in this release: the Clock pack, the weather refinement, and the Large and Extra-large size classes for bigger TVs (planned for 0.6.5).

## 0.6.3: Roomier layout, a bigger preview, and status icons
- **Layout size** (Settings, Appearance): Compact (6 apps a row), Comfortable (5, the default) or Cozy (4, larger tiles with more space).
  Every size keeps the same 50-unit side margins, so the row never runs off a bigger screen.
- **Bigger corners and spacing**: tiles have a 24-unit radius (was 16), gaps grow with the size, and the tab bar and Settings text are larger.
- **Settings** has more air: 18-unit row titles, a 56-unit row height with 8 units between rows (no hairlines), larger section titles, and
  a slim scroll bar beside the rows that shows where you are in a long list. A part of the next row is visible at the bottom, so the list
  does not look cut off.
- **Status icons** beside the clock: Wi-Fi with its bars (or Ethernet), an amber crossed Wi-Fi when there is no network, a lock while a VPN
  is on, and a crossed speaker while the sound is muted. Settings, Status icons: "Always" or "Only problems".
- **Debug builds only**: Settings, Preview size shows the launcher laid out at 1080p or 4K and scaled to your 720p TV, to check how a bigger
  screen looks. Release builds have no such row.
- The hero backdrop is shorter and fades more gently into the wallpaper.
- Under the hood: `Metrics` holds the tile sizes (one place, unit tested), `Screen` the screen size the layout is built for, and
  `DisplaySettings` the new Appearance rows. Tests: 85.

## 0.6.2: A roomier Settings, and a bar that switches at once
- **Settings redesigned** in the usual settings layout: the five sections down the left, and on the right one panel with the section's title,
  a sentence about it and its rows, with the help for the row in focus underneath (and a picture for the wallpaper rows). Rows are one panel
  with hairlines between them instead of a card each, with 8-unit spacing throughout. Down the sections shows each at once; Right enters the
  rows, Left or Back returns to the sections. This replaces the first page of 0.6.0, which wasted the screen.
- **Fixed**: Up on the second Settings row jumped to the tab bar instead of the first row. Up now moves to the previous row, and only the
  first row goes to the bar.
- **The tab bar behaves like tabs**: Left and Right show the neighbouring page at once (150 ms) while the focus stays on the bar; Down goes
  into the page (into the sections list on Settings). OK still opens a page and moves into it.

## 0.6.1: Safety (from checking couch-tv)
- **The setup scripts no longer switch off the TV's own home or setup apps.** couch-tv found that switching off Google TV's home and setup
  apps can leave some TVs unable to restart (a blank screen and a loop); our scripts did the same. Now `shangos.sh home`, `setup.sh` and
  `setup.ps1` choose shangOS as the home screen and leave the TV's own home on. If the TV still opens its own home, open shangOS from the app
  row. `--disable-stock` (`-DisableStock`) remains for experts: it asks first, warns about the loop, and never touches the setup app.
- Running them again **switches back on** anything an older version switched off, and `status` warns if something is still off.
- **Crash guard**: if shangOS crashes twice within five minutes, it hands Home to the TV's own home and tries again later, so a broken
  build cannot hold a TV in a restart loop. The crash times are a few numbers kept on the TV.
- The setup page has a new section, "If your TV loops after a restart", with the recovery steps.
- `AGENTS.md` (rules for AI agents and new contributors) and `tools/privacy-scan.sh` (no private addresses, MACs, e-mails or keys in the repo).
- `PLAN.md` has what we learned from couch-tv and Couchy. Tests: 82.

## 0.6.0: Settings in pages
- **Settings is now a short first page** (Appearance, Motion and sound, Home, Privacy, System). Each group is a page of its own and Back
  returns to the first page, with the group you left selected. Dialogs (wallpaper, name, city, performance, widgets) return to the page
  they were opened from.
- **Home, Apps order**: the Apps tab can be Manual (your own order, as before), A to Z, or Recently used. Home keeps your own order. On a
  sorted Apps tab, holding OK opens the app's menu instead of lifting the tile.
- **Home, Hidden apps**: lists the apps hidden from Home; choose one to show it again.
- **Your city also sets the sunrise and sunset** that switch Daybreak between its light and dark pictures (and the screen saver's), not just
  the weather. Without a chosen city it is still the time zone's main city.
- Under the hood: `SettingsScreen` keeps the pages, the list and the preview card; `SettingsPages` builds the rows of each group;
  `AppsSettings` has the order and hidden-apps rows. Tests: 77.


## 0.5.0: Polish
- **Frosted glass**: the tab bar, cards, menus and the Settings preview show a blurred copy of your wallpaper behind them. The wallpaper is
  blurred once into a tiny picture when it loads and each panel draws its part of it, so it costs next to nothing and lines up exactly.
  Settings, Frosted glass turns it off; it is also off on the Light performance setting.
- **Hero backdrop**: when an app has something to resume, its artwork fades softly into the wallpaper behind the greeting.
- **Calm mode**: after 20 quiet seconds the tab bar and clock fade back so the wallpaper has the room; any key brings them back. Settings,
  Calm mode turns it off.
- **Shimmer** placeholders instead of "Finding your apps..." while the list is read; **rise-in**: the greeting, widgets and tiles rise into
  place one after another when the launcher starts.
- The shadow under the focused card takes the colour of the app's icon.
- **Status icons** beside the clock only when they matter: a lock while a VPN is on, an amber crossed Wi-Fi when offline.
- No Lottie library: it needs AppCompat and adds up to 1 MB. Native animation is used instead (see `docs/dev/PLAN.md`).
- Under the hood: `Frost`, `Backdrop`, `Shimmer`, `StatusIcons`, `Calm`, `ScreenSaverSettings`; `MainActivity` lost its motion/sound wrappers.
  The project plan for 0.6 to 1.0 is in `docs/dev/PLAN.md`. Tests: 76.

## 0.4.6: Your city, from a list inside the app
- **Settings, City** and a new setup step, **Where is this TV?**: choose your city for the weather from a list built into the app (about
  23,000 towns and cities of 15,000 people or more). Type a name (accents and case don't matter, a later word matches too, "portland, us"
  narrows by country); results show the biggest first. No location service, no permission, no lookup of the TV's address and no network:
  choosing a city sends nothing anywhere. Skip it, or choose Automatic, to keep the main city of the TV's time zone.
- The list is not hand-written. `tools/cities.js` generates `res/raw/cities.txt` from GeoNames (CC BY 4.0, credited in Settings, About,
  Licences) as packaged by the community all-the-cities package; run it again to pick up newer data.
- Automatic Celsius/Fahrenheit follows the country of the chosen city. The window no longer pans under the keyboard (from Luma 2.5).
- From Luma 2.5 we did not take the automatic city from the TV's internet address, nor its online city search. See `NOTICE.md`.
- `SettingsScreen` shrinks again: the City and Temperature rows moved to `WeatherSettings`. Tests: 72.

## 0.4.5: Grab to move, Uninstall, and your own photo
- **Grab to move**: hold OK on a tile and it lifts; move it with the d-pad and press OK to put it down (Back puts it down where it is). It
  works on Home and on the Apps page, and hidden or uninstalled apps in between never make a move a no-op. Put down without moving it,
  the tile's menu opens as before.
- **Uninstall** in the menu of an app you installed (not for the TV's own apps); Android shows its own confirmation. The freed menu space is
  from dropping "Move left" and "Move right". This needs the normal `REQUEST_DELETE_PACKAGES` permission, listed on the privacy page and in
  the audit.
- **Your photo** as wallpaper: `./shangos.sh wallpaper ~/photo.jpg` hands a picture to the app's private storage over adb. No media
  permission, no account, no network. It becomes the look "Your photo", and the screen saver shows it dimmed. `--remove` deletes it. The
  receiving provider is exported but protected by the DUMP permission, which only the adb shell holds, and the audit checks that.
- Docs: the guide no longer mentions a Menu key that did nothing.

## 0.4.4: Launch as a departure
- Opening an app from a tile now grows its window out of the card (the system draws it, nothing runs in shangOS meanwhile). With Fluid motion
  off it simply appears, as before.

## 0.4.3: The hero knows what you were doing, and the room follows your focus
- **Resume hero**: focus an app that has something to continue and the line under its name reads "Continue: Stranger Things · S4 E3 ·
  18 min left", with the programme's picture at the right. Apps with nothing to resume still say when you last opened them. It uses the
  Continue watching list you already allowed for Up Next (and the same remote-poster setting for pictures); nothing new is requested.
- **Room lighting**: the light behind your apps takes the colour of the app you are on (Netflix warms to red, Spotify to green) and the
  wallpaper dims a little while focus is on the apps. Two cross-fading gradients, nothing per frame. Settings, Room lighting turns it off;
  it is also off on the Light performance setting.
- Under the hood: `AppLauncher` (opening apps and the long-press menu), `WatchNext`/`Resume`/`Posters` (Up Next now shares them),
  `StageLight`. Tests: 60.

## 0.4.2: Return where you were, and tell the truth
- **Return where you were**: open an app and come back (Home or Back) and focus is on that app's tile, not on YouTube. The hero reads
  "Opened just now". Pressing Home while on Home still returns to the first app and the greeting.
- **The hero stays true**: "Opened 2 minutes ago" counts up while you sit on the tile (checked at the start of every minute), and the
  greeting's date follows midnight.
- **Clock and date follow the TV**: the Home clock uses the TV's 12/24-hour setting; the date uses the TV's language. The screen saver
  shares the same code, so all three agree.
- Copy: Settings said "Silver" and "silver silk to the Milky Way"; the default wallpaper is Daybreak and the list runs from Daybreak to
  Calm Sea. README status updated.

## 0.4.1: MainActivity split (no behaviour change)
- `MainActivity` went from 2,334 to under 800 lines. Each job now has its own class: `AppList` (order, hiding and recents, unit tested),
  `AppPages`, `AppTile`, `Hero`, `Chrome` (tabs and clock), `SettingsScreen`, `WallpaperPicker`, `NameEditor`, `HomeScreen`, `Feedback`,
  `Performance` and `DaylightSync`, each with one narrow `Host` interface instead of the whole Activity.
- `tools/lint.sh` now fails if `MainActivity` grows past 800 lines or any other non-widget class past 700.
- Tests: 54 (adds `AppList`).
- Removed code that was written but never read.

## 0.4.0: Home that is shangOS's own
- **Adaptive tiles**: every app gets the same glass card tinted from its own icon (dominant colour, weighted by saturation), with its banner or its icon on a
  soft light. The hand-drawn art for six named apps and the per-app colour and tagline tables inherited from Luma are gone.
- **Honest hero**: the line under the focused app is real information ("Opened 2 hours ago", from the times shangOS recorded), not marketing copy.
- **shangOS Ambient screen saver**: a large clock and date over a dimmed copy of your wallpaper, moving to a new place every minute (no pixel stays lit).
  No network, no permission. Settings has a Screen saver row; `shangos.sh ambient on|off|preview` sets it on TVs that hide the choice, and removing
  shangOS puts the previous screen saver back first.
- **Setup guide**: new "Pick a look" step (6 steps now); the wallpaper shows through while you choose.
- Audit rule: every exported service must require a system-only (`BIND_`) permission.
- Tests: 45 (colour extraction, relative time). Fixed a thread-ordering slip in the new accent code before release.

## 0.3.0
- **New package id `dev.madusha.shangos`** (matches shangos.madusha.dev). An install of an earlier id is a different app: remove it and install this one;
  settings do not carry over.
- **Wallpaper engine replaced.** One picture in a plain hardware view instead of continuously redrawn surface layers; movement is render-thread
  animation that stops the moment a key is pressed. The picture's memory is graphics-only. Fjord, Dusk and Graphite scenes and the water mesh are gone.
- **New wallpapers**: Daybreak (light by day, dark after sunset), Aurora, Ember, Tide, Orchid, Glacier, made for this project, plus two CC0 photos.
  Stored as 4:4:4 JPEG (WebP was measured to band visibly on dark gradients). All upstream artwork is gone.
- **Hardware check**: Settings, Performance Auto picks Light, Balanced or Quality from memory, bitness and cores; the choice and the reasons are shown.
- **Faster to appear**: the last list of apps is kept on disk, so tiles show at once on launch and refresh behind.
- **New icon and TV banner** (adaptive icon with a monochrome layer for themed icons).
- **Open-source licences** screen (Settings, About, Licences) with full texts and SPDX identifiers; About moved to its own class.
- **Setup guide**: new Performance step and a QR code to the website's default-launcher guide on the last step.
- **`shangos.sh wizard`**: lists TVs, accepts any `host:port` (remembered if you choose), picks edition, installs a local or checksum-verified release APK,
  sets the home screen, grants permissions, and a clean-up menu. Removing shangOS first gives Home back to the TV's own launcher.
- **Releases**: a `v*` tag builds, checks, signs (key only in a protected environment), verifies the certificate and reproducibility, and publishes with
  checksums and a provenance attestation. Actions are pinned to commit SHAs; CI uses the SDK already on the runner.
- **Website** draft in `docs/` (static, no third-party requests) with a brief for its builder; developer docs moved to `docs/dev/`.
- APK 4.3 MB to 2.4 MB. Tests: 35.

## 0.2.2: setup guide fixes, editor extraction, formatting
- **Setup guide**: Back now goes to the previous step instead of ending the whole guide (Back on the first step leaves it).
  Progress is saved, so leaving for a system settings page, or by mistake, resumes where it stopped; Settings shows "Step N of 4".
  A Back press that arrives just after returning from a system page is ignored, focus returns to the row you left, and when the
  system will no longer show the permission dialog the app's permission page opens instead of a dead button.
- **Overlays**: a cancel action that opens the next overlay no longer has its focus trap undone right afterwards.
- **Code**: `WidgetEditor`, `AppArt` and `Shine` moved out of `MainActivity` (2,752 to 2,238 lines after formatting); one `ScreenHost`
  interface shared by the Privacy screen and the editor instead of two copies.
- **Formatting**: all Java formatted with google-java-format (`--aosp`) and shell with shfmt, in a separate commit with no behaviour
  change (the compiled output is byte-identical). `tools/format.sh` now passes `--aosp` to match `.editorconfig`. Shellcheck findings in
  `build.sh`, `shangos.sh` and `test/run.sh` fixed, so `tools/lint.sh` passes.
- Tests: 23.

## 0.2.1: setup guide
- **Setup guide** (first run, and Settings -> Setup guide): welcome, privacy choices, optional permissions one at a time with live state,
  home-screen check. Skippable; Back counts as skip. `Overlays.valueRow` is now shared by it and the Privacy screen.
- `reportFullyDrawn()` once the first list of apps is on screen.

## 0.2.0 — Phase 2: speed, memory and structure (not yet tested on a device)
Renamed to shangOS (`com.madushas.shangos`).
**Performance** (each item names the cost it removes; validate on a real device)
- App list is read only when a package changes (receiver, debounced), not on every return to Home. Before, every return
  re-queried all apps and decoded every icon and banner, then discarded them if nothing had changed.
- Icons and banners decode on first draw, off the main thread, into byte-bounded LRU caches (2 MB / 3 MB, half on
  low-RAM TVs) that shrink on memory pressure. A failed icon load is remembered so it cannot loop.
- Wallpaper: replaced pictures are recycled once faded out (they waited for GC); the Fjord source images are freed after
  composing (about 12 MB); stale picker previews are skipped instead of decoded; the draw thread reads each bitmap once
  (fixes a race with the UI thread); everything is released at the system's background trim level and restored on return.
- Ambient wallpaper pauses after 10 minutes without a key press (30 on Quality) and resumes on the next one.
- New Settings row: Performance (Auto / Quality / Light). Light keeps the wallpaper still; Auto chooses by the TV's RAM.
- Fjord PNGs converted to lossless WebP (pixel-identical, measured): APK 5.0 MB to 4.3 MB.
- Art no longer allocates Paths while drawing.
**Code**
- Split MainActivity (1,388 to 965 lines): Motion, Spring, Theme, Ui, Prefs, App, AppMeta, AppRepo, Wallpaper, Overlays,
  PrivacyScreen. One spring implementation instead of three, one set of view helpers instead of two.
- Prefs is the only class that knows preference keys; it has a schema version and migrations.
- Modern APIs, zero compiler deprecation warnings: WindowInsetsController, OnBackInvokedCallback (API 33+) with the legacy
  path on 31-32, ActivityOptions instead of overridePendingTransition, unsafeCheckOpNoThrow, Activity.getDisplay.
- Fixed: a user-visible string damaged by my own rename ("Clouds Motion.DRIFT"), caught before release.
- Tests: 21 (Net, Spring against the original formula, Prefs including migration). Audit gained structure rules.

## 0.1.0 — Phase 1: security and privacy core (not yet tested on a device)
Forked from Luma TV 2.4.
- Renamed to shangOS (`dev.madusha.shangos`); `minSdk` 31 (Android 12), `targetSdk` 34.
- **New `Net`**: the only class that opens connections. https only, port 443, no credentials in URLs, every
  resolved address must be public (checked again on each redirect, max 2), content-type and size caps, one
  generic User-Agent. Weather JSON is locked to `api.open-meteo.com`. 11 unit tests plus a live check.
- Up Next posters and Now Playing art now go through `Net`; `file:` and `http:` links are refused; pictures are
  decoded sampled and refused above 16 megapixels.
- Cleartext traffic disabled (network security config, no exceptions).
- **Removed House Control**: Binder client, WebView, localhost server, scene logic (about 400 lines).
- **Weather is opt-in** (off by default) and its disclosure is shown where you turn it on.
- Remote posters setting: Off / Wi-Fi and Ethernet only (default) / Always. **Offline mode** switch.
- **Privacy screen**: network switch, opt-ins, live permission states, signing-certificate fingerprint.
- Demo/QA intent extras work only in the debug build (the activity is exported).
- Dropped `ACCESS_WIFI_STATE` (signal strength now comes from the connection itself).
- Build variants: `debug`, `release`, `offline` (no INTERNET permission at all).
- Removed the author's donation content, unused 1.9 MB image, House Control `<queries>` entry.
- Added local policy and device-measurement tooling, plus `test/` (JUnit).
