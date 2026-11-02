package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.PlaybackState
import com.example.model.RepeatMode
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MusicControllerConnection(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentQueueTracks: List<Track> = emptyList()
    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PinkWaveMediaService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                setupPlayerListener(controller)
                updateStateFromController()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(player: MediaController?) {
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                    updateCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val isBuffering = state == Player.STATE_BUFFERING
                val duration = player.duration.coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = if (duration > 0) duration else it.durationMs
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val track = currentQueueTracks.find { it.id == mediaId }
                val index = currentQueueTracks.indexOfFirst { it.id == mediaId }
                _playbackState.update {
                    it.copy(
                        currentTrack = track ?: it.currentTrack,
                        queueIndex = if (index >= 0) index else it.queueIndex,
                        currentPositionMs = 0L,
                        durationMs = track?.durationMs ?: it.durationMs
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playbackState.update { it.copy(repeatMode = mode) }
            }
        })
    }

    private fun updateStateFromController() {
        val player = mediaController ?: return
        val isPlaying = player.isPlaying
        val duration = player.duration.coerceAtLeast(0L)
        val pos = player.currentPosition.coerceAtLeast(0L)
        val currentMediaId = player.currentMediaItem?.mediaId
        val track = currentQueueTracks.find { it.id == currentMediaId }

        _playbackState.update {
            it.copy(
                isPlaying = isPlaying,
                currentTrack = track ?: it.currentTrack,
                currentPositionMs = pos,
                durationMs = if (duration > 0) duration else it.durationMs,
                shuffleMode = player.shuffleModeEnabled,
                repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            )
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track), startIndex: Int = queue.indexOf(track)) {
        val player = mediaController ?: return
        currentQueueTracks = if (queue.isNotEmpty()) queue else listOf(track)
        val targetIndex = if (startIndex in currentQueueTracks.indices) startIndex else 0

        val mediaItems = currentQueueTracks.map { t ->
            MediaItem.Builder()
                .setMediaId(t.id)
                .setUri(Uri.parse(t.mediaUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .setArtworkUri(if (t.artworkUri.isNotBlank()) Uri.parse(t.artworkUri) else null)
                        .build()
                )
                .build()
        }

        player.setMediaItems(mediaItems, targetIndex, 0L)
        player.prepare()
        player.play()

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = currentQueueTracks,
                queueIndex = targetIndex,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && currentQueueTracks.isNotEmpty()) {
                val index = _playbackState.value.queueIndex.coerceAtLeast(0)
                val currentTrack = currentQueueTracks.getOrNull(index) ?: currentQueueTracks.first()
                playTrack(currentTrack, currentQueueTracks, index)
            } else {
                player.play()
            }
        }
    }

    fun skipToNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && currentQueueTracks.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val player = mediaController ?: return
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaController ?: return
        player.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleShuffle() {
        val player = mediaController ?: return
        val newMode = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newMode
    }

    fun cycleRepeatMode() {
        val player = mediaController ?: return
        val current = _playbackState.value.repeatMode
        val (nextPlayerMode, nextMode) = when (current) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL to RepeatMode.ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE to RepeatMode.ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF to RepeatMode.OFF
        }
        player.repeatMode = nextPlayerMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun playQueueItem(index: Int) {
        val player = mediaController ?: return
        if (index in currentQueueTracks.indices) {
            player.seekTo(index, 0L)
            player.play()
            _playbackState.update {
                it.copy(
                    queueIndex = index,
                    currentTrack = currentQueueTracks[index]
                )
            }
        }
    }

    fun addToQueue(track: Track) {
        val player = mediaController ?: return
        currentQueueTracks = currentQueueTracks + track
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.mediaUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(if (track.artworkUri.isNotBlank()) Uri.parse(track.artworkUri) else null)
                    .build()
            )
            .build()
        player.addMediaItem(mediaItem)
        _playbackState.update { it.copy(queue = currentQueueTracks) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateCurrentPosition()
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateCurrentPosition() {
        val player = mediaController ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        _playbackState.update {
            it.copy(
                currentPositionMs = pos,
                durationMs = if (dur > 0) dur else it.durationMs
            )
        }
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
