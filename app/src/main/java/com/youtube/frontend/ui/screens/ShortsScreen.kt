package com.youtube.frontend.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.PlayerView
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _shorts = MutableStateFlow<List<Video>>(emptyList())
    val shorts: StateFlow<List<Video>> = _shorts

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadShorts()
    }

    fun loadShorts() {
        viewModelScope.launch {
            _isLoading.value = true
            // Search for shorts
            repository.search("shorts viral")
                .onSuccess { videos ->
                    _shorts.value = videos.map { it.copy(isShort = true) }
                }
                .onFailure {
                    _shorts.value = listOf(
                        Video("dQw4w9WgXcQ", "This is a Shorts video #shorts", thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", isShort = true, channelName = "Shorts Creator"),
                        Video("jNQXAC9IVRw", "Amazing trick #shorts #viral", thumbnailUrl = "https://i.ytimg.com/vi/jNQXAC9IVRw/hqdefault.jpg", isShort = true, channelName = "Viral Shorts"),
                        Video("9bZkp7q19f0", "Funny moment 😂 #shorts", thumbnailUrl = "https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg", isShort = true, channelName = "Comedy Shorts")
                    )
                }
            _isLoading.value = false
        }
    }

    fun playShort(videoId: String, url: String) {
        playerManager.playVideo(videoId, url)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    onVideoClick: (String) -> Unit,
    viewModel: ShortsViewModel = hiltViewModel()
) {
    val shorts by viewModel.shorts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pagerState = rememberPagerState(pageCount = { shorts.size })

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (shorts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No shorts found")
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = shorts[page]
            ShortsPlayerItem(
                video = video,
                onLike = {},
                onComment = { onVideoClick(video.id) },
                onShare = {}
            )
        }

        // Top bar
        TopAppBar(
            title = { Text("Shorts", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                }
            }
        )
    }
}

@Composable
fun ShortsPlayerItem(
    video: Video,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Video player would go here - using placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            // In real app: ExoPlayer view with vertical video
            Text(
                text = "▶ ${video.title}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(32.dp)
            )
        }

        // Right side actions
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLike) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("12K", color = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLike) {
                    Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Dislike", color = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onComment) {
                    Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("1.2K", color = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Share", color = Color.White)
            }
        }

        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("@${video.channelName}", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(video.title, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Original audio • ${video.channelName}", color = Color.White)
            }
        }
    }
}
