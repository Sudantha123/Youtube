package com.youtube.frontend.data

import com.google.gson.annotations.SerializedName

// Simplified YouTube Models for UI

data class Video(
    val id: String,
    val title: String,
    val channelId: String = "",
    val channelName: String = "",
    val channelAvatar: String = "",
    val viewCount: String = "",
    val viewCountText: String = "",
    val publishedTime: String = "",
    val duration: String = "",
    val durationSeconds: Int = 0,
    val thumbnailUrl: String = "",
    val isLive: Boolean = false,
    val isShort: Boolean = false,
    val description: String = "",
    val likeCount: String = "",
    val subscriberCount: String = ""
)

data class Channel(
    val id: String,
    val name: String,
    val handle: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val subscriberCount: String = "",
    val videoCount: String = "",
    val description: String = "",
    val isVerified: Boolean = false
)

data class Playlist(
    val id: String,
    val title: String,
    val channelName: String = "",
    val videoCount: Int = 0,
    val thumbnailUrl: String = "",
    val videos: List<Video> = emptyList()
)

data class Comment(
    val id: String,
    val author: String,
    val authorAvatar: String,
    val text: String,
    val likeCount: String,
    val publishedTime: String,
    val replyCount: Int = 0
)

data class SearchSuggestion(
    val text: String
)

data class StreamData(
    val videoId: String,
    val title: String,
    val author: String,
    val hlsUrl: String? = null,
    val dashUrl: String? = null,
    val muxedStreams: List<Format> = emptyList(),
    val videoOnlyStreams: List<Format> = emptyList(),
    val audioOnlyStreams: List<Format> = emptyList(),
    val subtitles: List<Subtitle> = emptyList()
)

data class Format(
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val width: Int? = null,
    val height: Int? = null,
    val qualityLabel: String? = null,
    val audioQuality: String? = null,
    val url: String,
    val contentLength: String? = null
)

data class Subtitle(
    val language: String,
    val url: String
)

// Innertube Request/Response wrappers

data class InnertubeContext(
    val client: InnertubeClient
)

data class InnertubeClient(
    val clientName: String,
    val clientVersion: String,
    val hl: String = "en",
    val gl: String = "US",
    val androidSdkVersion: Int? = null,
    val osName: String? = null,
    val osVersion: String? = null
)

data class BrowseRequest(
    val context: InnertubeContext,
    val browseId: String,
    val params: String? = null
)

data class SearchRequest(
    val context: InnertubeContext,
    val query: String,
    val params: String? = null
)

data class PlayerRequest(
    val context: InnertubeContext,
    val videoId: String,
    val playbackContext: Map<String, Any>? = null,
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true
)

data class NextRequest(
    val context: InnertubeContext,
    val videoId: String
)

data class SearchSuggestionsRequest(
    val context: InnertubeContext,
    val input: String
)

// For local DB
data class HistoryEntry(
    val videoId: String,
    val watchedAt: Long,
    val progress: Long = 0
)

data class WatchLater(
    val videoId: String,
    val addedAt: Long
)
