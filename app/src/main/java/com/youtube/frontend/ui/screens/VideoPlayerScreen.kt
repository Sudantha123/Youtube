package com.youtube.frontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.youtube.frontend.data.StreamData
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.db.AppDatabase
import com.youtube.frontend.db.HistoryEntity
import com.youtube.frontend.db.LikedVideoEntity
import com.youtube.frontend.db.WatchLaterEntity
import com.youtube.frontend.player.PlayerManager
import com.youtube.frontend.ui.components.CompactVideoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val playerManager: PlayerManager,
    private val db: AppDatabase
) : ViewModel() {

    private val _videoId = MutableStateFlow<String?>(null)
    private val _streamData = MutableStateFlow<StreamData?>(null)
    val streamData: StateFlow<StreamData?> = _streamData

    private val _relatedVideos = MutableStateFlow<List<Video>>(emptyList())
    val relatedVideos: StateFlow<List<Video>> = _relatedVideos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _isInWatchLater = MutableStateFlow(false)
    val isInWatchLater: StateFlow<Boolean> = _isInWatchLater

    val exoPlayer get() = playerManager.exoPlayer

    fun loadVideo(videoId: String) {
        _videoId.value = videoId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Check liked/watch later status
            _isLiked.value = db.likedVideosDao().exists(videoId)
            _isInWatchLater.value = db.watchLaterDao().exists(videoId)

            repository.getVideoStream(videoId)
                .onSuccess { stream ->
                    _streamData.value = stream
                    // Play video
                    val bestUrl = stream.hlsUrl
                        ?: stream.muxedStreams.maxByOrNull { it.bitrate }?.url
                        ?: stream.videoOnlyStreams.firstOrNull()?.url

                    if (bestUrl != null) {
                        if (bestUrl.contains("m3u8") || stream.hlsUrl != null) {
                            playerManager.playHls(bestUrl, videoId)
                        } else {
                            playerManager.playVideo(videoId, bestUrl)
                        }
                    }

                    // Save to history
                    db.historyDao().insert(
                        HistoryEntity(
                            videoId = videoId,
                            title = stream.title,
                            channelName = stream.author,
                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        )
                    )

                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to load video"
                    _isLoading.value = false
                }

            // Load related
            repository.getRelatedVideos(videoId)
                .onSuccess { _relatedVideos.value = it }
        }
    }

    fun toggleLike(video: Video) {
        viewModelScope.launch {
            if (_isLiked.value) {
                db.likedVideosDao().delete(video.id)
                _isLiked.value = false
            } else {
                db.likedVideosDao().insert(
                    LikedVideoEntity(
                        videoId = video.id,
                        title = video.title,
                        channelName = video.channelName,
                        thumbnailUrl = video.thumbnailUrl
                    )
                )
                _isLiked.value = true
            }
        }
    }

    fun toggleWatchLater(video: Video) {
        viewModelScope.launch {
            if (_isInWatchLater.value) {
                db.watchLaterDao().delete(video.id)
                _isInWatchLater.value = false
            } else {
                db.watchLaterDao().insert(
                    WatchLaterEntity(
                        videoId = video.id,
                        title = video.title,
                        channelName = video.channelName,
                        thumbnailUrl = video.thumbnailUrl
                    )
                )
                _isInWatchLater.value = true
            }
        }
    }

    override fun onCleared() {
        // Don't release player here, keep for background playback
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val streamData by viewModel.streamData.collectAsState()
    val relatedVideos by viewModel.relatedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val isInWatchLater by viewModel.isInWatchLater.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(streamData?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Player
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .then(if (isLoading) Modifier else Modifier)
                ) {
                    if (viewModel.exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = viewModel.exoPlayer
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isLoading) CircularProgressIndicator()
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadVideo(videoId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Video info
            item {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = streamData?.title ?: "Loading...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://i.pravatar.cc/100?u=${streamData?.author}",
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(streamData?.author ?: "", fontWeight = FontWeight.Medium)
                            Text("1.2M subscribers", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { /* subscribe */ }) {
                            Text("Subscribe")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val currentVideo = Video(
                            id = videoId,
                            title = streamData?.title ?: "",
                            channelName = streamData?.author ?: "",
                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        )
                        FilterChip(
                            selected = isLiked,
                            onClick = { viewModel.toggleLike(currentVideo) },
                            label = { Text(if (isLiked) "Liked" else "Like") },
                            leadingIcon = { Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* dislike */ },
                            label = { Text("Dislike") },
                            leadingIcon = { Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = isInWatchLater,
                            onClick = { viewModel.toggleWatchLater(currentVideo) },
                            label = { Text("Save") },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* share */ },
                            label = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("1.2M views • 2 days ago", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                "This video was extracted using InnerTune API (Innertube). Full YouTube features supported including adaptive streaming, comments, and related videos.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Comments header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Comments • 1.2K", fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                Divider()
            }

            // Related videos
            item {
                Text(
                    "Up next",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            items(relatedVideos) { video ->
                CompactVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (relatedVideos.isEmpty() && !isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No related videos found")
                    }
                }
            }
        }
    }
}
