# InnerTune API Integration - Technical Documentation

## Overview
මේ app එක YouTube official Data API එක පාවිච්චි කරන්නේ නැහැ. ඒ වෙනුවට InnerTune project එකේ වගේ YouTube ගේ private Innertube API එක reverse engineer කරලා පාවිච්චි කරනවා.

## What is Innertube?
YouTube website, Android app, iOS app ඇතුළු සියලුම official clients ලා පාවිච්චි කරන්නේ `youtubei/v1/*` endpoints. මේක තමයි Innertube.

Official Data API එකට වඩා:
- ✅ No quota limits
- ✅ No API key needed (public key `AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8`)
- ✅ More data - hidden metrics, streaming URLs, recommendations
- ✅ Same data as YouTube app

## Endpoints Used

### 1. Browse - Home Feed, Trending, Categories
```
POST https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8
Body: {
  "context": { "client": { "clientName": "WEB", "clientVersion": "2.20240101.00.00" } },
  "browseId": "FEwhat_to_watch" // Home
}
```
Browse IDs:
- `FEwhat_to_watch` - Home
- `FEtrending` - Trending
- `FEmusic` - Music
- `FEgaming` - Gaming
- `FElive` - Live
- `FEnews` - News
- `FEsports` - Sports
- `FEsubscriptions` - Subscriptions (needs auth)

### 2. Search
```
POST /youtubei/v1/search
Body: {
  "context": {...},
  "query": "search term"
}
```
Response contains `twoColumnSearchResultsRenderer` with `videoRenderer`, `channelRenderer`, `playlistRenderer`

### 3. Player - Video Streaming URLs
```
POST /youtubei/v1/player
Body: {
  "context": {
    "client": {
      "clientName": "ANDROID",
      "clientVersion": "19.09.37",
      "androidSdkVersion": 30
    }
  },
  "videoId": "dQw4w9WgXcQ"
}
```
Returns:
- `streamingData.hlsManifestUrl` - HLS for ExoPlayer
- `streamingData.dashManifestUrl` - DASH
- `streamingData.formats` - Progressive MP4
- `streamingData.adaptiveFormats` - Video-only + Audio-only (need muxing or ExoPlayer handles)

**Why ANDROID client?**
- WEB client returns ciphered URLs needing signature decipher (n param, s param)
- ANDROID client returns direct URLs without cipher (easier)
- IOS client also no cipher, but ANDROID more stable

### 4. Next - Related Videos
```
POST /youtubei/v1/next
Body: {
  "context": {...},
  "videoId": "dQw4w9WgXcQ"
}
```
Returns `secondaryResults` with `compactVideoRenderer` list

### 5. Search Suggestions (Optional)
```
GET https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=query
```

## Parsing Logic - The Hard Part

Innertube responses are **deeply nested** and designed for UI rendering, not clean data.

Example path to home videos:
```
contents.twoColumnBrowseResultsRenderer.tabs[0].tabRenderer.content.richGridRenderer.contents[].richItemRenderer.content.videoRenderer
```

Our parser tries multiple paths:
1. `richGridRenderer` (new YouTube layout)
2. `sectionListRenderer` (old layout fallback)
3. `itemSectionRenderer` (search)

Each `videoRenderer` contains:
- `videoId`
- `title.runs[0].text`
- `thumbnail.thumbnails[]` (pick highest resolution)
- `ownerText.runs[0].text` (channel name)
- `viewCountText.simpleText`
- `publishedTimeText.simpleText`
- `lengthText.simpleText` (duration)

## Streaming - ExoPlayer Integration

```kotlin
val hlsUrl = streamingData.hlsManifestUrl
val mediaItem = MediaItem.Builder()
    .setUri(hlsUrl)
    .setMimeType("application/x-mpegURL")
    .build()
exoPlayer.setMediaItem(mediaItem)
exoPlayer.prepare()
exoPlayer.play()
```

For adaptive formats (video-only + audio-only), ExoPlayer can handle DASH manifest automatically if you provide `dashManifestUrl`.

## Authentication (Future)

For Subscriptions, History, Liked videos from YouTube account:
- Need OAuth or cookie-based auth
- `SAPISID`, `__Secure-3PAPISID`, `LOGIN_INFO`
- Currently we use local Room DB for those features (offline-first)
- To add real auth: copy cookies from logged-in browser

## InnerTune vs This Project

| Feature | InnerTune (Music) | This Project (Video) |
|---------|-------------------|----------------------|
| Client | YouTube Music | YouTube |
| Browse IDs | `FEmusic_*` | `FEwhat_to_watch`, etc |
| Player | Audio-only | Video+Audio, HLS/DASH |
| UI | Music player | YouTube main app clone |
| Extra | Lyrics, queue | Shorts, Comments, Channel |

## Rate Limits & Bans

- Innertube has **very high** rate limits (used by millions of YouTube clients)
- No official quota like Data API (10k units/day)
- But YouTube can still block IP if abusive
- Use reasonable delays, caching (we cache with 5-30 min TTL)

## Security Notes

- Never log `playerResponse` fully - contains sensitive URLs
- `signatureCipher` and `n` param are YouTube's anti-bot
- ANDROID client bypasses cipher, but YouTube may change this anytime
- Pin exact client versions, monitor for breakages

## Future Improvements

1. **SABR** - New YouTube streaming protocol (segmented, like DASH but better)
2. **PO Token** - Proof of Origin token needed for some clients now (2024+)
3. **Visitor Data** - Generate visitorData for unauthenticated requests
4. **Comments API** - Use `next` with continuation for comments
5. **Live Chat** - WebSocket for live streams

## References

- InnerTune: https://github.com/z-huang/InnerTune
- InnerTubeX: https://github.com/MetrolistGroup/innertubex
- Python InnerTube: https://github.com/tombulled/innertube
- NewPipeExtractor: https://github.com/TeamNewPipe/NewPipeExtractor
