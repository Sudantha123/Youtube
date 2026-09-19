package com.youtube.frontend.data

import com.google.gson.JsonObject
import com.youtube.frontend.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * InnerTune / Innertube API - YouTube's private API
 * This is the core that powers the YouTube frontend without official API keys
 * Based on reverse-engineered InnerTune project logic
 */

interface InnertubeService {
    @POST("youtubei/v1/browse")
    suspend fun browse(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject

    @POST("youtubei/v1/search")
    suspend fun search(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject

    @POST("youtubei/v1/player")
    suspend fun player(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject

    @POST("youtubei/v1/next")
    suspend fun next(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject

    @POST("youtubei/v1/search")
    suspend fun getSearchSuggestions(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject

    @POST("youtubei/v1/channel")
    suspend fun channel(
        @Query("key") apiKey: String = Constants.INNERTUBE_API_KEY,
        @Body body: Map<String, Any>
    ): JsonObject
}

object InnertubeApi {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip")
                .addHeader("X-YouTube-Client-Name", "3")
                .addHeader("X-YouTube-Client-Version", Constants.CLIENT_VERSION_ANDROID)
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Referer", "https://www.youtube.com/")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.INNERTUBE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: InnertubeService = retrofit.create(InnertubeService::class.java)

    fun getWebContext(): Map<String, Any> = mapOf(
        "context" to mapOf(
            "client" to mapOf(
                "clientName" to Constants.CLIENT_NAME_WEB,
                "clientVersion" to Constants.CLIENT_VERSION_WEB,
                "hl" to "en",
                "gl" to "US"
            )
        )
    )

    fun getAndroidContext(): Map<String, Any> = mapOf(
        "context" to mapOf(
            "client" to mapOf(
                "clientName" to Constants.CLIENT_NAME_ANDROID,
                "clientVersion" to Constants.CLIENT_VERSION_ANDROID,
                "androidSdkVersion" to Constants.ANDROID_SDK_VERSION,
                "hl" to "en",
                "gl" to "US",
                "osName" to "Android",
                "osVersion" to "11"
            )
        )
    )

    fun getIOSContext(): Map<String, Any> = mapOf(
        "context" to mapOf(
            "client" to mapOf(
                "clientName" to Constants.CLIENT_NAME_IOS,
                "clientVersion" to Constants.CLIENT_VERSION_IOS,
                "hl" to "en",
                "gl" to "US",
                "deviceModel" to "iPhone14,3",
                "osName" to "iPhone",
                "osVersion" to "17.0.1.20C66"
            )
        )
    )

    // Parser helpers - extract videos from complex Innertube response
    fun parseVideosFromBrowseResponse(json: JsonObject): List<Video> {
        val videos = mutableListOf<Video>()
        try {
            // The response structure is deeply nested, we attempt multiple paths
            val contents = json
                .getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("richGridRenderer")
                ?.getAsJsonArray("contents")

            contents?.forEach { item ->
                try {
                    val richItem = item.asJsonObject.getAsJsonObject("richItemRenderer")
                    val videoRenderer = richItem?.getAsJsonObject("content")
                        ?.getAsJsonObject("videoRenderer")
                    if (videoRenderer != null) {
                        parseVideoRenderer(videoRenderer)?.let { videos.add(it) }
                    }
                } catch (_: Exception) {}
            }

            // Fallback: try sectionListRenderer path
            if (videos.isEmpty()) {
                val sectionContents = json
                    .getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                    ?.getAsJsonArray("tabs")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("tabRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")

                sectionContents?.forEach { section ->
                    try {
                        val itemSection = section.asJsonObject.getAsJsonObject("itemSectionRenderer")
                        val itemContents = itemSection?.getAsJsonArray("contents")
                        itemContents?.forEach { contentItem ->
                            val videoRenderer = contentItem.asJsonObject.getAsJsonObject("videoRenderer")
                            if (videoRenderer != null) {
                                parseVideoRenderer(videoRenderer)?.let { videos.add(it) }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return videos
    }

    fun parseVideosFromSearchResponse(json: JsonObject): List<Video> {
        val videos = mutableListOf<Video>()
        try {
            val contents = json
                .getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnSearchResultsRenderer")
                ?.getAsJsonObject("primaryContents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("itemSectionRenderer")
                ?.getAsJsonArray("contents")

            contents?.forEach { item ->
                try {
                    val obj = item.asJsonObject
                    when {
                        obj.has("videoRenderer") -> {
                            parseVideoRenderer(obj.getAsJsonObject("videoRenderer"))?.let { videos.add(it) }
                        }
                        obj.has("channelRenderer") -> {
                            // Can parse channel but we skip for video list
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return videos
    }

    fun parseVideoRenderer(renderer: JsonObject): Video? {
        return try {
            val videoId = renderer.get("videoId")?.asString ?: return null
            val title = renderer.getAsJsonObject("title")
                ?.getAsJsonArray("runs")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
                ?: renderer.getAsJsonObject("title")?.get("simpleText")?.asString
                ?: "Unknown"

            val channelName = renderer.getAsJsonObject("ownerText")
                ?.getAsJsonArray("runs")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: ""

            val channelId = renderer.getAsJsonObject("ownerText")
                ?.getAsJsonArray("runs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
                ?.get("browseId")?.asString ?: ""

            val viewCount = renderer.getAsJsonObject("viewCountText")
                ?.get("simpleText")?.asString
                ?: renderer.getAsJsonObject("viewCountText")
                    ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                ?: ""

            val publishedTime = renderer.getAsJsonObject("publishedTimeText")
                ?.get("simpleText")?.asString ?: ""

            val thumbnailUrl = renderer.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.let { thumbs ->
                    if (thumbs.size() > 0) thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString else ""
                } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            val duration = renderer.getAsJsonObject("lengthText")
                ?.get("simpleText")?.asString ?: ""

            val isLive = renderer.has("badges") && renderer.getAsJsonArray("badges").toString().contains("LIVE")

            Video(
                id = videoId,
                title = title,
                channelId = channelId,
                channelName = channelName,
                viewCountText = viewCount,
                viewCount = viewCount,
                publishedTime = publishedTime,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                isLive = isLive
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseStreamData(json: JsonObject, videoId: String): StreamData? {
        return try {
            val videoDetails = json.getAsJsonObject("videoDetails")
            val title = videoDetails?.get("title")?.asString ?: ""
            val author = videoDetails?.get("author")?.asString ?: ""

            val streamingData = json.getAsJsonObject("streamingData")
            val hlsUrl = streamingData?.get("hlsManifestUrl")?.asString
            val dashUrl = streamingData?.get("dashManifestUrl")?.asString

            val formats = mutableListOf<Format>()
            val adaptiveFormats = mutableListOf<Format>()

            streamingData?.getAsJsonArray("formats")?.forEach { f ->
                try {
                    val obj = f.asJsonObject
                    formats.add(
                        Format(
                            itag = obj.get("itag")?.asInt ?: 0,
                            mimeType = obj.get("mimeType")?.asString ?: "",
                            bitrate = obj.get("bitrate")?.asInt ?: 0,
                            width = obj.get("width")?.asInt,
                            height = obj.get("height")?.asInt,
                            qualityLabel = obj.get("qualityLabel")?.asString,
                            url = obj.get("url")?.asString ?: "",
                            contentLength = obj.get("contentLength")?.asString
                        )
                    )
                } catch (_: Exception) {}
            }

            streamingData?.getAsJsonArray("adaptiveFormats")?.forEach { f ->
                try {
                    val obj = f.asJsonObject
                    adaptiveFormats.add(
                        Format(
                            itag = obj.get("itag")?.asInt ?: 0,
                            mimeType = obj.get("mimeType")?.asString ?: "",
                            bitrate = obj.get("bitrate")?.asInt ?: 0,
                            width = obj.get("width")?.asInt,
                            height = obj.get("height")?.asInt,
                            qualityLabel = obj.get("qualityLabel")?.asString,
                            audioQuality = obj.get("audioQuality")?.asString,
                            url = obj.get("url")?.asString ?: "",
                            contentLength = obj.get("contentLength")?.asString
                        )
                    )
                } catch (_: Exception) {}
            }

            val videoOnly = adaptiveFormats.filter { it.mimeType.contains("video") && !it.mimeType.contains("audio") || it.width != null }
            val audioOnly = adaptiveFormats.filter { it.mimeType.contains("audio") || it.audioQuality != null }

            StreamData(
                videoId = videoId,
                title = title,
                author = author,
                hlsUrl = hlsUrl,
                dashUrl = dashUrl,
                muxedStreams = formats,
                videoOnlyStreams = videoOnly,
                audioOnlyStreams = audioOnly
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
