package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MusicRepository
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.playback.MusicControllerConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Playlists : Screen()
    object Favorites : Screen()
    object Library : Screen()
    object Settings : Screen()
    data class PlaylistDetail(val playlist: Playlist) : Screen()
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val controllerConnection = MusicControllerConnection(application)

    // Playback state from controller
    val playbackState: StateFlow<PlaybackState> = controllerConnection.playbackState

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Now playing expanded modal
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Queue drawer visibility
    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    // Online featured & trending tracks
    private val _featuredTracks = MutableStateFlow<List<Track>>(emptyList())
    val featuredTracks: StateFlow<List<Track>> = _featuredTracks.asStateFlow()

    private val _isLoadingFeatured = MutableStateFlow(false)
    val isLoadingFeatured: StateFlow<Boolean> = _isLoadingFeatured.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Local device audio tracks
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // Favorites & Playlists from Room DB
    val favorites: StateFlow<List<Track>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playbackHistory: StateFlow<List<Track>> = repository.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tracks in currently selected playlist
    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    // Equalizer preset
    private val _eqPreset = MutableStateFlow("Neon Bass Boost")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    // Sleep timer in minutes remaining (0 = inactive)
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Track to add to playlist dialog
    private val _trackToAddToPlaylist = MutableStateFlow<Track?>(null)
    val trackToAddToPlaylist: StateFlow<Track?> = _trackToAddToPlaylist.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        loadFeaturedOnlineTracks()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun setQueueVisible(visible: Boolean) {
        _isQueueVisible.value = visible
    }

    fun loadFeaturedOnlineTracks(tag: String = "electronic") {
        viewModelScope.launch {
            _isLoadingFeatured.value = true
            val tracks = repository.getFeaturedOnlineTracks(tag)
            _featuredTracks.value = tracks
            _isLoadingFeatured.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val results = repository.searchOnlineTracks(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _searchError.value = "No Creative Commons tracks found for \"$query\""
                }
            } catch (e: Exception) {
                _searchError.value = "Failed to connect to music service: ${e.localizedMessage}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun setStoragePermissionGranted(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            loadDeviceAudioTracks()
        }
    }

    fun loadDeviceAudioTracks() {
        viewModelScope.launch {
            val tracks = repository.getDeviceAudioTracks()
            _localTracks.value = tracks
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        controllerConnection.playTrack(track, queue)
        viewModelScope.launch {
            repository.recordHistory(track)
        }
    }

    fun togglePlayPause() {
        controllerConnection.togglePlayPause()
    }

    fun skipToNext() {
        controllerConnection.skipToNext()
    }

    fun skipToPrevious() {
        controllerConnection.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controllerConnection.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controllerConnection.toggleShuffle()
    }

    fun cycleRepeatMode() {
        controllerConnection.cycleRepeatMode()
    }

    fun playQueueItem(index: Int) {
        controllerConnection.playQueueItem(index)
    }

    fun addToQueue(track: Track) {
        controllerConnection.addToQueue(track)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun isFavorite(trackId: String): Flow<Boolean> = repository.isFavorite(trackId)

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_currentScreen.value is Screen.PlaylistDetail) {
                _currentScreen.value = Screen.Playlists
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _currentScreen.value = Screen.PlaylistDetail(playlist)
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collect { tracks ->
                _selectedPlaylistTracks.value = tracks
            }
        }
    }

    fun showAddToPlaylistDialog(track: Track) {
        _trackToAddToPlaylist.value = track
    }

    fun dismissAddToPlaylistDialog() {
        _trackToAddToPlaylist.value = null
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            _trackToAddToPlaylist.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun setEqPreset(preset: String) {
        _eqPreset.value = preset
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60_000L)
                    remaining--
                    _sleepTimerMinutes.value = remaining
                }
                // Pause playback when timer expires
                controllerConnection.mediaController?.pause()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        controllerConnection.release()
    }
}
