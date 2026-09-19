package com.youtube.frontend.data

import com.google.gson.JsonObject
import com.youtube.frontend.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor() {

    private val api = InnertubeApi.service

    suspend fun getHomeFeed(): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["browseId"] = Constants.BROWSE_ID_HOME

            val response = api.browse(body = body)
            val videos = InnertubeApi.parseVideosFromBrowseResponse(response)

            if (videos.isNotEmpty()) {
                Result.success(videos)
            } else {
                // Fallback to trending
                getTrending()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTrending(): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["browseId"] = Constants.BROWSE_ID_TRENDING

            val response = api.browse(body = body)
            val videos = InnertubeApi.parseVideosFromBrowseResponse(response)
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryFeed(browseId: String): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["browseId"] = browseId

            val response = api.browse(body = body)
            val videos = InnertubeApi.parseVideosFromBrowseResponse(response)
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["query"] = query

            val response = api.search(body = body)
            val videos = InnertubeApi.parseVideosFromSearchResponse(response)
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSearchSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Use YouTube search suggest endpoint
            val suggestions = mutableListOf<String>()
            // For now return mock suggestions based on query
            // In real implementation, call https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$query
            suggestions.add(query)
            suggestions.add("$query songs")
            suggestions.add("$query official")
            suggestions.add("$query live")
            suggestions.add("$query tutorial")
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoStream(videoId: String): Result<StreamData> = withContext(Dispatchers.IO) {
        try {
            // Try ANDROID client first (no cipher needed)
            var body = InnertubeApi.getAndroidContext().toMutableMap()
            body["videoId"] = videoId

            var response = api.player(body = body)
            var streamData = InnertubeApi.parseStreamData(response, videoId)

            // Fallback to WEB if needed
            if (streamData == null || (streamData.muxedStreams.isEmpty() && streamData.hlsUrl == null)) {
                body = InnertubeApi.getWebContext().toMutableMap()
                body["videoId"] = videoId
                response = api.player(body = body)
                streamData = InnertubeApi.parseStreamData(response, videoId)
            }

            // Fallback to IOS
            if (streamData == null || (streamData.muxedStreams.isEmpty() && streamData.hlsUrl == null)) {
                body = InnertubeApi.getIOSContext().toMutableMap()
                body["videoId"] = videoId
                response = api.player(body = body)
                streamData = InnertubeApi.parseStreamData(response, videoId)
            }

            if (streamData != null) {
                Result.success(streamData)
            } else {
                Result.failure(Exception("Failed to extract stream"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getRelatedVideos(videoId: String): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["videoId"] = videoId

            val response = api.next(body = body)

            // Parse related videos from next response
            val videos = mutableListOf<Video>()
            try {
                val contents = response
                    .getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnWatchNextResults")
                    ?.getAsJsonObject("secondaryResults")
                    ?.getAsJsonObject("secondaryResults")
                    ?.getAsJsonArray("results")

                contents?.forEach { item ->
                    try {
                        val compactVideo = item.asJsonObject.getAsJsonObject("compactVideoRenderer")
                        if (compactVideo != null) {
                            val id = compactVideo.get("videoId")?.asString ?: return@forEach
                            val title = compactVideo.getAsJsonObject("title")?.get("simpleText")?.asString
                                ?: compactVideo.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                                ?: ""
                            val channelName = compactVideo.getAsJsonObject("longBylineText")
                                ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                            val thumb = compactVideo.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                                ?.let { if (it.size() > 0) it.get(it.size() - 1).asJsonObject.get("url")?.asString else "" }
                                ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"

                            videos.add(
                                Video(
                                    id = id,
                                    title = title,
                                    channelName = channelName,
                                    thumbnailUrl = thumb
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChannelDetails(channelId: String): Result<Channel> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["browseId"] = channelId

            val response = api.browse(body = body)

            // Parse channel header
            val header = response
                .getAsJsonObject("header")
                ?.getAsJsonObject("c4TabbedHeaderRenderer")

            val name = header?.getAsJsonObject("title")?.get("simpleText")?.asString ?: "Channel"
            val avatar = header?.getAsJsonObject("avatar")
                ?.getAsJsonArray("thumbnails")
                ?.let { if (it.size() > 0) it.get(it.size() - 1).asJsonObject.get("url")?.asString else "" } ?: ""
            val banner = header?.getAsJsonObject("banner")
                ?.getAsJsonArray("thumbnails")
                ?.let { if (it.size() > 0) it.get(it.size() - 1).asJsonObject.get("url")?.asString else "" } ?: ""

            val channel = Channel(
                id = channelId,
                name = name,
                avatarUrl = avatar,
                bannerUrl = banner,
                subscriberCount = header?.getAsJsonObject("subscriberCountText")?.get("simpleText")?.asString ?: ""
            )

            Result.success(channel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChannelVideos(channelId: String): Result<List<Video>> = withContext(Dispatchers.IO) {
        try {
            val body = InnertubeApi.getWebContext().toMutableMap()
            body["browseId"] = channelId
            body["params"] = "EgZ2aWRlb3M%3D" // videos tab

            val response = api.browse(body = body)
            val videos = InnertubeApi.parseVideosFromBrowseResponse(response)
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
