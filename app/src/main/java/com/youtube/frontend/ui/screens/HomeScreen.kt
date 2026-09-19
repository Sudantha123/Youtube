package com.youtube.frontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getHomeFeed()
                .onSuccess { vids ->
                    _videos.value = vids.ifEmpty { getMockVideos() }
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _videos.value = getMockVideos() // fallback mock for demo
                    _error.value = null
                    _isLoading.value = false
                }
        }
    }

    fun selectCategory(category: CategoryChip) {
        _selectedCategory.value = category.id
        if (category.browseId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                repository.getCategoryFeed(category.browseId)
                    .onSuccess { vids ->
                        _videos.value = vids.ifEmpty { getMockVideos() }
                    }
                    .onFailure {
                        _videos.value = getMockVideos()
                    }
                _isLoading.value = false
            }
        } else {
            loadHomeFeed()
        }
    }

    fun getMockVideos(): List<Video> = listOf(
        Video("dQw4w9WgXcQ", "Never Gonna Give You Up - Official Music Video", "UCuAXFkgsw1L7xaCfnd5JJOw", "Rick Astley", viewCountText = "1.5B views", publishedTime = "14 years ago", duration = "3:33", thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"),
        Video("jNQXAC9IVRw", "Me at the zoo - First YouTube Video Ever", "UC4QobU6STFB0r_TGmWUYLCg", "jawed", viewCountText = "300M views", publishedTime = "19 years ago", duration = "0:19", thumbnailUrl = "https://i.ytimg.com/vi/jNQXAC9IVRw/hqdefault.jpg"),
        Video("9bZkp7q19f0", "PSY - GANGNAM STYLE(강남스타일) M/V", "UCrDkAvwZum-UTjHmzDI2iIw", "officialpsy", viewCountText = "5.4B views", publishedTime = "12 years ago", duration = "4:13", thumbnailUrl = "https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg"),
        Video("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "UCxoq-PBRUQS1d1wK-2ll38w", "Luis Fonsi", viewCountText = "8.3B views", publishedTime = "7 years ago", duration = "4:42", thumbnailUrl = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"),
        Video("OPf0YbXqDm0", "Mark Ronson - Uptown Funk ft. Bruno Mars", "UCMtFAi84ehTSYSE9XoHefig", "Mark Ronson", viewCountText = "5B views", publishedTime = "9 years ago", duration = "4:31", thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg"),
        Video("RgKAFK5djSk", "Wiz Khalifa - See You Again ft. Charlie Puth", "UCp0hYYBW6IMayGgRjLJ_bjA", "Wiz Khalifa", viewCountText = "6B views", publishedTime = "9 years ago", duration = "3:58", thumbnailUrl = "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBarYouTube(
                onSearchClick = onSearchClick,
                onNotificationClick = {},
                onProfileClick = {}
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CategoryChipsRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            when {
                isLoading -> LoadingIndicator()
                error != null -> ErrorMessage(message = error!!, onRetry = { viewModel.loadHomeFeed() })
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
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
}
