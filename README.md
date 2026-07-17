# Crack Monitoring Application

An Android client for real-time drone-based structural crack monitoring — built during an internship project. The app connects to a local Flask/YOLO streaming server over WebView, surfaces live crack-detection status, tracks stream latency, logs detections, and reports connection health through a persistent floating "mascot" overlay service.

> 📱 Android · Kotlin · minSdk 24

## Features

- **Live video feed** from a drone/camera stream via WebView, with drone-connection and YOLO crack-detection status indicators
- **Stream latency measurement** — samples round-trip timing to the streaming server
- **Detection log** — history of detections with a confidence gauge view and per-item detail screen
- **Floating mascot overlay service** — a persistent foreground service that surfaces live status outside the app
- **Configurable server settings** — separate stream/data endpoints (IP + port), editable at runtime rather than hardcoded
- **Auth flow** — login/register/profile screens with local session handling
- **Notifications** for detection/connection events
- **Light/dark theming** via a dedicated `ThemeManager`

## Tech stack

- Kotlin, Android Views (no Compose in this project)
- WebView-based video streaming client
- Kotlin Coroutines for async latency sampling and polling
- SharedPreferences for session/settings persistence
- Material Components (bottom navigation, swipe-to-refresh)

## Project structure

```
app/src/main/java/com/example/seismicaplication/
├── MainActivity.kt           # live stream screen, connection status
├── ServerConfig.kt           # single source of truth for stream/data endpoints
├── DetectionLog*.kt          # detection history list, adapter, storage
├── MascotService.kt/View.kt  # floating overlay service
├── Login/Register/Profile*.kt
└── SettingsActivity.kt       # configurable IP/port + polling interval
```

## Building it yourself

```bash
git clone <this-repo>
cd CrackMonitoringApplication
./gradlew assembleDebug
```

Requires JDK 17+ and the Android SDK. On first run, set the stream/data server IP and port from the in-app Settings screen — defaults point to a local development network and won't resolve outside it.

## Status

Built as part of an internship (magang) project. Server IP/port default to placeholder local-network values — point them at your own Flask/YOLO streaming backend to use it end-to-end.
