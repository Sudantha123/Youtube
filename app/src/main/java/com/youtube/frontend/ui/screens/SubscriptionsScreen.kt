package com.youtube.frontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.db.AppDatabase
import com.youtube.frontend.ui.components.VideoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val db: AppDatabase
) : ViewModel() {

    val subscriptions = db.subscriptionDao().getSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSubscriptionsFeed()
    }

    fun loadSubscriptionsFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            // For demo, load trending as subscription feed
            repository.getTrending()
                .onSuccess { _videos.value = it }
                .onFailure { _videos.value = emptyList() }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Subscriptions") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Subscribed channels row
            item {
                if (subscriptions.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(subscriptions) { sub ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(64.dp)
                            ) {
                                AsyncImage(
                                    model = sub.avatarUrl.ifEmpty { "https://i.pravatar.cc/100?u=${sub.channelId}" },
                                    contentDescription = sub.channelName,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    sub.channelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Divider()
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(videos) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video.id) },
                        onChannelClick = { onChannelClick(video.channelId) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
