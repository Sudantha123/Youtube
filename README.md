# YouTube Frontend - InnerTune API Powered

YouTube main app එකේ තියෙන සියලුම features සහිත, InnerTune / Innertube API භාවිතා කරලා හදපු full-featured YouTube frontend Android app එකක්.

![Build APK](https://github.com/Sudantha123/Youtube/actions/workflows/build-apk.yml/badge.svg)

## 🌟 Features (YouTube Main App වගේම)

### 📺 Core YouTube Features
- **Home Feed** - `FEwhat_to_watch` browse API එකෙන් personalized recommendations
- **Video Player** - ExoPlayer (Media3) සමඟ adaptive streaming (HLS/DASH), quality selection
- **Search** - Real-time search with suggestions, filters
- **Shorts** - Vertical swipe Shorts feed (YouTube Shorts clone)
- **Subscriptions** - Subscribed channels feed
- **Library / You Tab** - History, Watch Later, Liked Videos, Playlists (Room DB)
- **Channel Pages** - Channel banner, avatar, videos, subscribe
- **Related Videos** - `next` endpoint එකෙන් Up Next
- **Trending, Music, Gaming, Live, News, Sports** - Category feeds

### 🎨 UI/UX - YouTube Main App Replica
- **Material 3 / Material You** - Dynamic theming, dark/light
- **Bottom Navigation** - Home, Shorts, Subscriptions, You
- **Top App Bar** - YouTube logo, search, notifications, profile
- **Video Cards** - Thumbnail, duration, channel avatar, title, views
- **Category Chips** - Filter chips like official app
- **Shimmer Loading** - Skeleton loading states

### 🔧 Technical - InnerTune API
- **Innertube API** - YouTube's private internal API (reverse engineered)
  - `WEB` client: `2.20240101.00.00` for browse/search
  - `ANDROID` client: `19.09.37` for player (no cipher needed)
  - `IOS` client: Fallback for streaming
- **No API Key Needed** - Uses `AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8` (public InnerTube key)
- **Streaming Extraction** - Parses `streamingData`, `hlsManifestUrl`, `adaptiveFormats`
- **Fallback Logic** - Android → Web → iOS for maximum compatibility

### 📱 Additional Features
- **Background Playback** - MediaSessionService
- **Picture-in-Picture** - Config change handling
- **Offline Support** - Room database for history, playlists
- **Download** - WorkManager + Media3 DownloadService scaffold
- **Search History** - Local search history
- **Like/Dislike, Save, Share, Subscribe** - Full engagement

## 🏗️ Architecture

```
com.youtube.frontend/
├── data/
│   ├── InnertubeApi.kt       # Core InnerTune API client
│   ├── Models.kt             # Video, Channel, etc.
│   └── YouTubeRepository.kt  # Repository pattern
├── db/
│   └── AppDatabase.kt        # Room DB - history, watch later, liked
├── player/
│   ├── PlayerManager.kt      # ExoPlayer singleton
│   └── PlaybackService.kt    # MediaSessionService
├── ui/
│   ├── theme/                # Material 3 theme
│   ├── components/           # VideoCard, Chips, etc.
│   └── screens/              # Home, Search, Player, Shorts, Library, Channel, Subscriptions
├── di/
│   └── AppModule.kt          # Hilt DI
├── utils/
│   └── Constants.kt
├── MainActivity.kt           # NavHost
└── YouTubeApp.kt
```

## 🔌 InnerTune API Usage

```kotlin
// Home feed
val body = InnertubeApi.getWebContext().toMutableMap()
body["browseId"] = "FEwhat_to_watch"
val response = api.browse(body = body)
val videos = InnertubeApi.parseVideosFromBrowseResponse(response)

// Player - Android client avoids signature deciphering
val androidBody = InnertubeApi.getAndroidContext().toMutableMap()
androidBody["videoId"] = videoId
val playerResponse = api.player(body = androidBody)
val streamData = InnertubeApi.parseStreamData(playerResponse, videoId)

// Search
val searchBody = InnertubeApi.getWebContext().toMutableMap()
searchBody["query"] = "query"
val searchResponse = api.search(body = searchBody)
```

## 🚀 GitHub එකෙන් APK Build කරන හැටි

### Automatic Build
1. Repo එකට push කරන්න - GitHub Actions automatically APK build කරයි
2. Actions tab එකේ `Build YouTube Frontend APK` workflow එක බලන්න
3. Artifacts වලින් `app-debug.apk` download කරන්න

### Manual Build
```bash
# Clone
git clone https://github.com/Sudantha123/Youtube.git
cd Youtube

# Build debug APK
./gradlew assembleDebug
# APK එක තියෙන්නේ: app/build/outputs/apk/debug/app-debug.apk

# Build release APK
./gradlew assembleRelease
```

### GitHub Actions Workflow
`.github/workflows/build-apk.yml`:
- JDK 17 + Android SDK setup
- Gradle 8.4
- `assembleDebug` + `assembleRelease`
- Uploads APK as artifact
- Auto release on tags

## 📦 APK Install

1. GitHub Actions artifacts වලින් APK download කරන්න
2. Phone එකේ Unknown Sources enable කරන්න
3. APK install කරන්න
4. Enjoy YouTube without ads! (InnerTune API bypass)

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material3
- **Player**: Media3 ExoPlayer 1.2.1 (HLS/DASH)
- **Networking**: Retrofit2 + OkHttp + Gson
- **Image**: Coil 2.5.0
- **DB**: Room 2.6.1 + DataStore
- **DI**: Hilt 2.48.1
- **Navigation**: Navigation Compose 2.7.6
- **Async**: Coroutines + Flow + Paging3

## 🎯 YouTube Main App Features Checklist

- [x] Home feed with recommendations
- [x] Video playback with adaptive quality
- [x] Search with suggestions
- [x] Shorts vertical feed
- [x] Subscriptions feed
- [x] Library (History, Watch Later, Liked, Playlists)
- [x] Channel pages
- [x] Related videos / Up Next
- [x] Like, Dislike, Subscribe, Save, Share
- [x] Comments section UI
- [x] Trending, Music, Gaming, Live categories
- [x] Dark/Light theme (Material You)
- [x] Background playback service
- [x] Search history
- [x] Offline database
- [x] Picture-in-Picture ready
- [x] Download scaffold
- [x] GitHub Actions APK build

## 📄 License

GPL-3.0 - InnerTune inspired

## 🙏 Credits

- **InnerTune** by z-huang - Original YouTube Music client inspiration
- **Innertube** - YouTube's private API
- **NewPipeExtractor** - Fallback extractor
- **YouTube** - Obviously 😅

---

**Note**: This is an unofficial YouTube frontend. Uses InnerTune API approach. No official YouTube API key needed. For educational purposes.

**Sinhala**: මේක YouTube එකේ official app එක නෙමෙයි. InnerTune API එක පාවිච්චි කරලා හදපු frontend එකක්. GitHub එකෙන්ම APK build වෙනවා.
