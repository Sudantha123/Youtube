package com.youtube.frontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.youtube.frontend.data.Channel
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.db.AppDatabase
import com.youtube.frontend.db.SubscriptionEntity
import com.youtube.frontend.ui.components.VideoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val _channel = MutableStateFlow<Channel?>(null)
    val channel: StateFlow<Channel?> = _channel

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _isSubscribed.value = db.subscriptionDao().exists(channelId)

            repository.getChannelDetails(channelId)
                .onSuccess { _channel.value = it }
                .onFailure {
                    _channel.value = Channel(channelId, "Channel $channelId", avatarUrl = "https://i.pravatar.cc/100?u=$channelId")
                }

            repository.getChannelVideos(channelId)
                .onSuccess { _videos.value = it }
                .onFailure { _videos.value = emptyList() }

            _isLoading.value = false
        }
    }

    fun toggleSubscribe() {
        viewModelScope.launch {
            val channel = _channel.value ?: return@launch
            if (_isSubscribed.value) {
                db.subscriptionDao().delete(channel.id)
                _isSubscribed.value = false
            } else {
                db.subscriptionDao().insert(
                    SubscriptionEntity(
                        channelId = channel.id,
                        channelName = channel.name,
                        avatarUrl = channel.avatarUrl
                    )
                )
                _isSubscribed.value = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelId: String,
    onVideoClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val channel by viewModel.channel.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()

    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel?.name ?: "Channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && channel == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // Banner
                AsyncImage(
                    model = channel?.bannerUrl?.ifEmpty { "https://picsum.photos/800/200?random=${channel?.id}" },
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = channel?.avatarUrl?.ifEmpty { "https://i.pravatar.cc/100?u=${channel?.id}" },
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("@${channel?.name?.lowercase()?.replace(" ", "")} • ${channel?.subscriberCount ?: "1M subscribers"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(channel?.description ?: "Welcome to ${channel?.name} channel! Subscribe for latest videos.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.toggleSubscribe() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface,
                                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(if (isSubscribed) "Subscribed" else "Subscribe")
                        }
                        OutlinedButton(onClick = {}) {
                            Text("Join")
                        }
                    }
                }
                Divider()
            }

            item {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("Videos") })
                    Tab(selected = false, onClick = {}, text = { Text("Shorts") })
                    Tab(selected = false, onClick = {}, text = { Text("Live") })
                    Tab(selected = false, onClick = {}, text = { Text("Playlists") })
                }
            }

            if (videos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No videos found")
                    }
                }
            } else {
                items(videos) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video.id) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
