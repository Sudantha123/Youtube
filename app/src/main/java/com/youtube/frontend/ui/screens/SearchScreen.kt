package com.youtube.frontend.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.frontend.data.Video
import com.youtube.frontend.data.YouTubeRepository
import com.youtube.frontend.db.AppDatabase
import com.youtube.frontend.db.SearchHistoryEntity
import com.youtube.frontend.ui.components.CompactVideoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = db.searchHistoryDao().getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.length >= 2) {
            viewModelScope.launch {
                repository.getSearchSuggestions(newQuery)
                    .onSuccess { _suggestions.value = it }
            }
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun search(query: String = _query.value) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            db.searchHistoryDao().insert(SearchHistoryEntity(query = query))
            repository.search(query)
                .onSuccess { videos ->
                    _searchResults.value = videos
                }
                .onFailure {
                    _searchResults.value = emptyList()
                }
            _isLoading.value = false
            _suggestions.value = emptyList()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            db.searchHistoryDao().clearAll()
        }
    }

    fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            db.searchHistoryDao().delete(query)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onVideoClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val history by viewModel.searchHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search YouTube") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = { viewModel.search() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when {
                suggestions.isNotEmpty() && results.isEmpty() -> {
                    LazyColumn {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onQueryChange(suggestion)
                                        viewModel.search(suggestion)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(suggestion)
                            }
                        }
                    }
                }
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                results.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(results) { video ->
                            CompactVideoCard(video = video, onClick = { onVideoClick(video.id) })
                        }
                    }
                }
                else -> {
                    // Search history
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent searches", style = MaterialTheme.typography.titleMedium)
                            if (history.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                    Text("Clear all")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        history.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.search(item.query) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(item.query, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeHistoryItem(item.query) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Text("No recent searches", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
