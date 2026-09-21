# Pocket Server

> Turn your Android phone into a personal local server.

Open app → **Start Server** → share the URL → access your phone from any laptop or browser on the same Wi-Fi. No cloud, no account — your files never leave your local network.

```text
http://192.168.1.20:8080
```

---

## Table of Contents

- [Features](#features)
- [App Tour](#app-tour)
- [Design System](#design-system)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Browser Web UI](#browser-web-ui)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [Permissions](#permissions)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [License](#license)

---

## Features

### HTTP File Server (primary)
- Browse, download, upload, create folder, rename, delete, search, sort
- Only exposes folders **you** select via the Storage Access Framework — never `/data`, `/system`, etc.
- Streaming uploads/downloads (64 KB buffer, never loads whole files into RAM)
- HTTP Range support for video/audio seeking (`206 Partial Content`, `416` handling)
- 10 GB per-file guard, duplicate handling (`file (1).txt`), 20 files per request cap

### Browser Web UI
- Dark-first glass interface (sidebar on desktop, bottom nav on mobile)
- File explorer with list/grid toggle, breadcrumbs, debounced search, sorting
- Drag-anywhere-to-upload with per-file progress drawer (XHR, cancellable)
- Photos gallery + lightbox (prev/next, keyboard nav, download)
- Media browser with native video/audio playback
- Multi-select with contextual action bar, custom context menu, file-details drawer
- Polished login page, toasts (no `alert()`), skeletons, empty/error states
- Connection-info modal, `prefers-color-scheme` light mode, reduced-motion support

### In-app Photos & Media
- **Photos tab**: 3-column grid of images from shared folders (Coil thumbnails), full-screen pager viewer, share via system sheet
- **Media tab**: videos + audio grouped, handed off to the device player via granted content URI
- Read-only walk over the existing storage layer (depth-limited, 500-item cap)

### Web Server Mode
- Select a folder containing `index.html` + assets
- Served as a static site at `http://PHONE_IP:8080/` with range-aware streaming
- Canonicalized paths, traversal-protected

### FTP Server (optional plugin)
- Independent toggle: Settings → FTP server
- Configurable port (default 2121), username, shared-folder root, same hashed password as HTTP auth
- Minimal RFC-959 subset: `USER/PASS, PWD/CWD/CDUP, PASV/PORT, LIST/NLST, RETR/STOR, DELE/MKD/RMD/RNFR/RNTO, SIZE/MDTM`
- Loosely coupled — HTTP and FTP run independently

### Authentication & Recovery
- Enabled by default: `admin / admin` (change on first use)
- Passwords stored as salted SHA-256 ×120k — never plaintext, never logged
- Secure random session tokens (`HttpOnly` cookie, 60-min default expiry, `Bearer` fallback)
- Rate limiting: 5 failures / 60 s → 5-minute block with `Retry-After`
- **Reset sign-in** (Settings → Security): restores `admin/admin`, signs out all browsers, clears login blocks
- Toggle auth off takes effect **live** — open browser tabs detect it on focus, no reload needed

### Server Dashboard
- Animated status hero (Offline / Starting / Running / Error)
- One morphing CTA: Start ⇄ Stop (same button, same slot)
- Live URL with tap-for-QR + copy, Open/Share actions
- Shared-folders + real device-storage cards, recent-activity timeline, connection helper with Refresh / Test-in-browser / detected-IP diagnostics

### Foreground Service
- `dataSync` foreground service with persistent notification (`Server running — 192.168.x.x:8080`, STOP action, tap to open app)
- Survives backgrounding; handles Wi-Fi disconnect/reconnect, port conflicts, crashes without killing the app

### Logs, Activity & Developer
- Activity tab (Today / Yesterday / Older sticky groups, capped lazy rows)
- Full server logs (Room-persisted, 300 cap, clearable, per-row status tints)
- Developer screen with copyable technical summary

### Settings
- Server name, HTTP/FTP ports (validated 1024–65535), auto-start, background server
- Authentication toggle, username/password change, session timeout, reset sign-in
- Shared folders (via Files tab), web-server root picker, FTP toggle
- Appearance: System / Dark / Light; Advanced: logs, developer mode

### Monetization-ready
- `FeatureAccessManager` gate (`Free`: HTTP + media, 3 folders · `Pro`: everything unlimited) — no scattered `if (pro)` checks

---

## App Tour

```text
Home                          Files                         Activity
┌────────────────────┐        ┌────────────────────┐        ┌────────────────────┐
│ Pocket Server  🔔⚙ │        │ Files              │        │ Activity           │
│ Your phone…        │        │ ▸ Search folders…  │        │ Today              │
│ ┌────────────────┐ │        │ ┌────────────────┐ │        │ ● Server started   │
│ │○ SERVER OFFLINE│ │        │ │ 📁 DCIM        │ │        │   8:21 PM          │
│ │Start your      │ │        │ │ 📁 Documents   │ │        │ ─────────────────  │
│ │server     [srv]│ │        │ └────────────────┘ │        │ Yesterday          │
│ │[▶ Start Server]│ │        │         (+)       │        │ ↑ photo.jpg        │
│ │Files Photos…   │ │        └────────────────────┘        └────────────────────┘
│ └────────────────┘ │
│ [Folders] [Store]  │  Bottom dock (all tabs): Home · Files · Activity · Settings
│ Recent Activity…   │  floating glass pill, active capsule highlight
└────────────────────┘
```

---

## Design System

AMOLED black + white, Apple-like type. No blue anywhere in the app.

| Token | Dark | Light |
|---|---|---|
| Background | `#000000` pure black | `#F5F5F7` |
| Surface / elevated | `#0B0B0D` / `#131316` | White / `#EDEDEF` |
| Primary (buttons, active tab, links) | White / black text | Near-black / white text |
| Text secondary / tertiary | `#A1A1A6` / `#6E6E73` | `#5B5F68` / `#8E8E93` |
| Borders | White 8% hairlines | Gray hairlines |
| Status only | Green running · red error | Same, darker |

- **Type**: Inter (bundled 400/500/600/700, system fallback), tight tracking on display sizes, semibold titles, monospace for IPs/ports/paths
- **Shape**: controls 10–14 · cards 18–24 · hero 28 · dock 30 · CTA 14–18
- **Motion**: 220 ms slide+fade navigation, pulsing status dot, skeleton loaders, `prefers-reduced-motion` respected on web

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language / build | Kotlin 2.0.21, AGP 8.7.3, Gradle Kotlin DSL + version catalog |
| UI | Jetpack Compose (BOM 2024.09.03), Material 3, Navigation Compose |
| HTTP server | Ktor 3.0.3 (CIO engine, ContentNegotiation, CORS, StatusPages) |
| Persistence | Room 2.6.1 (logs only), DataStore Preferences 1.1.1 (settings/creds/folders) |
| Async | Kotlin Coroutines 1.8.1, StateFlow |
| Media thumbs | Coil 2.6.0 (in-app photos only) |
| QR | ZXing (on-device generation, URL only — never credentials) |
| JSON | kotlinx.serialization 1.7.3 (server), org.json (small route payloads) |
| SDK | `compileSdk`/`targetSdk` 34, `minSdk` 26 |
| Package | `com.saketkhundia.pocketserver` |

---

## Architecture

Clean layers, server/UI loosely coupled:

```text
UI (Compose + Material3)
 ↓ StateFlow
ViewModel (Home / Settings / Logs / Media)
 ↓
Domain (models · ValidatePortUseCase · FeatureAccessManager)
 ↓
Repositories (Settings · SharedFolders · ServerState · AuthCredentials)
 ↓
Server Engine / Storage
(HttpServerManager [Ktor CIO] · FtpServerManager · StorageManager [SAF] ·
 SharedFolderManager [virtual paths] · AuthManager/Sessions/RateLimiter)
```

Dependency rule: nothing in `server/` knows about Compose; nothing in `presentation/` touches sockets or files directly.

---

## Project Structure

```text
app/src/main/java/com/saketkhundia/pocketserver/
├── MainActivity.kt · PocketServerApp.kt
├── di/AppContainer.kt                    # manual DI singletons
├── presentation/
│   ├── theme/        Theme.kt · Type.kt · Tokens.kt   # AMOLED system + Inter
│   ├── components/   Components.kt                   # glass kit: hero widgets, rows, states
│   ├── navigation/   NavGraph.kt                      # floating dock + transitions
│   ├── home/         HomeScreen.kt · HomeViewModel.kt # dashboard + server controls
│   ├── files/        FilesScreen.kt                   # shared-folder manager
│   ├── activity/     ActivityScreen.kt                # day-grouped timeline
│   ├── media/        MediaViewModel.kt · PhotosScreen.kt · MediaScreen.kt
│   ├── settings/     SettingsScreen.kt · SettingsViewModel.kt
│   ├── logs/ · developer/ · onboarding/ · qr/
├── server/
│   ├── HttpServerManager.kt              # lifecycle, bind, IP refresh, health
│   ├── routing/ApiRoutes.kt              # JSON API (see below)
│   ├── routing/WebUiAssets.kt            # embedded browser client
│   ├── auth/         AuthManager · SessionManager · PasswordHasher · RateLimiter
│   ├── security/     PathSecurity · RequestValidation
│   ├── media/        RangeSupport · MediaSupport
│   └── ftp/          FtpServerManager.kt
├── storage/        StorageManager · SharedFolderManager   # SAF only
├── network/        LocalIpProvider · NetworkManager
├── service/        ServerForegroundService.kt
├── data/           database/ · preferences/ · repository/
├── domain/         model/ · repository/ · usecase/ · monetization/
└── util/           Constants · FormatUtils · MimeUtils · StorageStats
app/src/main/res/font/                    # Inter 400/500/600/700 + family
app/src/test/                             # 35 unit + e2e tests
```

---

## Getting Started

### Requirements
- Android Studio (Hedgehog or newer), JDK 17+
- Android SDK Platform 34 (`compileSdk`/`targetSdk`), `minSdk` 26 device/emulator
- Phone + laptop on the **same Wi-Fi** for live testing

### Setup
```bash
git clone https://github.com/saketkhundia/Pocket-Server.git
cd Pocket-Server
echo "sdk.dir=/path/to/Android/Sdk" > local.properties   # never committed
./gradlew assembleDebug        # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on connected device
./gradlew testDebugUnitTest    # unit + server e2e tests
./gradlew assembleRelease      # release APK (minified; keep-rules in proguard-rules.pro)
```

> `local.properties` (your SDK path) is gitignored. Debug builds use `applicationIdSuffix ".debug"`, so debug and release can sit side by side.

---

## Usage Guide

1. **Add folders**: Files tab → `+` → pick folders (SAF picker; permissions are persistable and per-folder).
2. **Start**: Home → **Start Server** (button morphs to Stop Server; same slot).
3. **Connect**: open `http://PHONE_IP:PORT` on the laptop, or tap the URL → QR screen and scan it.
4. **Sign in**: default `admin / admin` — change it immediately in Settings → Security (username edit requires a new ≥4-char password).
5. **Serve a site**: put `index.html` in a shared folder → Settings → enable Web server mode → pick the folder.
6. **FTP**: Settings → enable FTP server → connect to `ftp://PHONE_IP:2121`.
7. **Locked out?** Settings → Security → **Reset sign-in** restores `admin/admin`, signs out browsers, clears login blocks.
8. **Turned auth off but browser still shows login?** Just focus the tab (or tap *"I turned auth off — check again"*); it re-checks `/api/status` and enters automatically.

---

## Browser Web UI

Served embedded at `/` (no build step — it's a single `WebUiAssets` template, no secrets inside):

- Desktop sidebar (Home / Files / Photos / Media + ● Connected modal), mobile bottom nav
- List/grid toggle, breadcrumbs, debounced search, 6 sort orders, 2000-item server cap
- Upload via button, drag-anywhere overlay, or empty-state CTA — per-file progress drawer over XHR
- Gallery lightbox (arrows/keyboard/download), native `<video>`/`<audio>` streaming over Range requests
- Multi-select action bar (download/delete), right-click menu / long-press details sheet, rename/delete/create-folder modals, toasts
- Breakpoints 767 / 1024 px, no horizontal scroll, touch-friendly targets, ARIA labels, focus states

---

## API Reference

Base `http://PHONE_IP:PORT` · JSON everywhere · auth via `Cookie: ps_session=…` or `Authorization: Bearer …`.

| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| `GET` | `/` | — | Web UI (or static site in web-server mode) |
| `GET` | `/ping`, `/health`, `/api/ping` | — | Diagnostics (`/health` returns server IP) |
| `GET` | `/api/status` | — | `{name, authRequired, authenticated, version}` |
| `POST` | `/api/auth/login` | — | `{username,password}` → `{ok,token}` + `Set-Cookie`; 401 bad creds, 429 + `Retry-After: 300` when blocked |
| `POST` | `/api/auth/logout` | — | Invalidates session, clears cookie |
| `GET` | `/api/files?path=&search=&sort=` | ✓ | `{path, files:[{name,path,isDir,size,lastModified,mime}]}`; sort `name(_desc)`/`size(_desc)`/`date(_desc)` |
| `GET` | `/api/media?type=image\|video&limit=` | ✓ | Recursive (depth ≤ 4) filtered listing |
| `GET`/`HEAD` | `/api/download?path=` | ✓ | Streaming + `Accept-Ranges`, `Content-Range`, inline disposition |
| `GET` | `/api/file?path=` | ✓ | Download alias |
| `POST` | `/api/upload?path=` | ✓ | `multipart/form-data` field `files`; 201 + `{uploaded:[…]}` |
| `POST` | `/api/folder` | ✓ | `{path,name}` → 201 / 409 exists |
| `PUT` | `/api/file` | ✓ | `{path,newName}` |
| `DELETE` | `/api/file?path=` | ✓ | Removes file/folder |
| `OPTIONS` | `/api/{...}` | — | `Allow` header |
| `GET` | `/{path...}` | ✓* | Static web-hosting catch-all (*login page served to browsers when unauthenticated) |

Status codes used: `200 · 201 · 204 · 400 · 401 · 403 · 404 · 409 · 413 · 416 · 429 · 500`.

---

## Security Model

| Threat | Mitigation |
|---|---|
| Path traversal (`../`, `%2e`, `\`, null bytes, double-encoding) | `PathSecurity.normalizeVirtualPath`: triple-decodes, rejects escapes above shared roots, length/segment caps |
| Unauthorized access | Every `/api/*` (except status/login/health/ping) enforces live session check; FTP verifies password hash |
| Directory escape / symlink-like tricks | SAF-only access: segment-by-segment resolution inside granted tree URIs; no raw filesystem paths cross the server boundary |
| Malicious filenames | Sanitizer rejects separators, `..`, null/control chars, trailing dots/spaces, >255 chars |
| Method abuse | Explicit allowlist: `GET HEAD POST PUT DELETE OPTIONS` |
| Giant uploads | 64 KB streaming, 20 files/request, 10 GB cap with partial-file cleanup, no `readBytes()` |
| Brute force | 5 fails / 60 s → 5-min per-IP block, 500 ms fail delay, `Retry-After` header |
| Secret leakage | Passwords hashed (salted SHA-256 ×120k); tokens random 32 B; logs sanitize `ps_session`; QR encodes URL only |

---

## Permissions

Minimal set — no contacts/location/camera/mic. Storage exclusively via the SAF folder picker.

`INTERNET · ACCESS_NETWORK_STATE · ACCESS_WIFI_STATE · FOREGROUND_SERVICE · FOREGROUND_SERVICE_DATA_SYNC · POST_NOTIFICATIONS · WAKE_LOCK · REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

---

## Testing

```bash
./gradlew testDebugUnitTest
```

35 tests, all green:

- `LoginFlowTest` — **real embedded Ktor server + real routes + real AuthManager**: login→cookie→authenticated files, 401, 429 + `Retry-After`, live auth-toggle reflection in `/api/status`
- `PathSecurityTest` — traversal, encoded/backslash/null-byte, unicode, oversize paths
- `AuthTest` — hasher, sessions, token extraction, limiter isolation
- `RangeSupportTest` · `ValidatePortTest` · `FeatureAccessTest` · `MimeUtilsTest`

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| Browser times out | Same Wi-Fi on both devices · disable VPN/mobile data · router AP isolation OFF · try `…/ping` and `…/health` · `ping PHONE_IP` from laptop · try port 8081 |
| Can't sign in | Default `admin/admin` · check caps/autocapitalize · wait 5 min after several failures (429) · Activity tab shows 401 vs 429 · **Reset sign-in** in Settings |
| Login page lingers after auth-off | Focus the tab / tap “check again” (it re-reads server state) |
| Port in use | Error card names the port — change HTTP/FTP port in Settings |
| Light theme | Settings → Appearance → Light (proper light hierarchy, not inverted) |
| No folders visible | Files tab → `+` → grant a folder; server only serves selected folders |

---

## Roadmap

- [ ] Thumbnail cache + virtualized large lists on web
- [ ] WebDAV / SMB / SFTP server plugins (extension points exist)
- [ ] Public sharing links, temporary links, server profiles
- [ ] Photo auto-backup, file sync, cloud relay / remote access
- [ ] Biometric app lock

---

## License

MIT — © 2026 saketkhundia. See `LICENSE` (to be added) for the full text.
