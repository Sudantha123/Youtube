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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.frontend.db.*
import com.youtube.frontend.ui.components.CompactVideoCard
import com.youtube.frontend.data.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val db: AppDatabase
) : ViewModel() {

    val history = db.historyDao().getHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val watchLater = db.watchLaterDao().getWatchLater().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val likedVideos = db.likedVideosDao().getLikedVideos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val subscriptions = db.subscriptionDao().getSubscriptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun clearHistory() {
        viewModelScope.launch { db.historyDao().clearAll() }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch { db.historyDao().delete(videoId) }
    }

    fun removeFromWatchLater(videoId: String) {
        viewModelScope.launch { db.watchLaterDao().delete(videoId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()
    val watchLater by viewModel.watchLater.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("History", "Watch Later", "Liked", "Playlists")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("You") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Profile header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@user • Manage your Google Account", style = MaterialTheme.typography.bodySmall)
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // History
                    if (history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No watch history")
                                Text("Videos you watch will appear here", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("History", fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Clear all")
                                    }
                                }
                            }
                            items(history) { entry ->
                                CompactVideoCard(
                                    video = Video(
                                        id = entry.videoId,
                                        title = entry.title,
                                        channelName = entry.channelName,
                                        thumbnailUrl = entry.thumbnailUrl
                                    ),
                                    onClick = { onVideoClick(entry.videoId) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Watch Later
                    if (watchLater.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No videos in Watch Later")
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(8.dp)) {
                            items(watchLater) { entry ->
                                CompactVideoCard(
                                    video = Video(entry.videoId, entry.title, channelName = entry.channelName, thumbnailUrl = entry.thumbnailUrl),
                                    onClick = { onVideoClick(entry.videoId) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Liked
                    if (likedVideos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No liked videos")
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(8.dp)) {
                            items(likedVideos) { entry ->
                                CompactVideoCard(
                                    video = Video(entry.videoId, entry.title, channelName = entry.channelName, thumbnailUrl = entry.thumbnailUrl),
                                    onClick = { onVideoClick(entry.videoId) }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Playlists
                    Column(modifier = Modifier.padding(16.dp)) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("New playlist")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Your playlists will appear here", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
