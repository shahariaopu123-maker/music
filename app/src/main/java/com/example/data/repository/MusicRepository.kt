package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.*
import com.example.data.remote.JamendoApiService
import com.example.model.Playlist
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val database: PinkWaveDatabase = PinkWaveDatabase.getInstance(context),
    private val jamendoApi: JamendoApiService = JamendoApiService.create()
) {
    private val dao = database.musicDao()

    // Curated legal Creative Commons fallback tracks (Jamendo & Free Archive CC-BY/CC0 music)
    private val curatedCCTracks = listOf(
        Track(
            id = "cc_1",
            title = "Pink Horizon",
            artist = "Neon Dreamer",
            album = "Cyber Synth Vol. 1",
            durationMs = 194000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1886884&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=391157&width=500",
            genre = "Synthwave",
            source = "Jamendo CC"
        ),
        Track(
            id = "cc_2",
            title = "Midnight Neon Wave",
            artist = "Aether Waves",
            album = "Retrowave Nights",
            durationMs = 212000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1888806&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=391583&width=500",
            genre = "Electronic",
            source = "Jamendo CC"
        ),
        Track(
            id = "cc_3",
            title = "Tokyo Pink Rain",
            artist = "Lofi Chroma",
            album = "Midnight Chill",
            durationMs = 168000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1887309&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=391277&width=500",
            genre = "Lo-Fi",
            source = "Jamendo CC"
        ),
        Track(
            id = "cc_4",
            title = "Glass Cathedral",
            artist = "Starlight Odyssey",
            album = "Ambient Dimensions",
            durationMs = 245000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1887201&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=391254&width=500",
            genre = "Ambient",
            source = "Jamendo CC"
        ),
        Track(
            id = "cc_5",
            title = "Electric Pulse",
            artist = "Vapour Pulse",
            album = "Cybernetic Heart",
            durationMs = 182000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1885992&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=390978&width=500",
            genre = "Dance",
            source = "Jamendo CC"
        ),
        Track(
            id = "cc_6",
            title = "Sunset Overdrive",
            artist = "Retro Skyline",
            album = "Outrun City",
            durationMs = 205000L,
            mediaUri = "https://prod-1.storage.jamendo.com/?trackid=1886890&format=mp32&from=app-dev",
            artworkUri = "https://usercontent.jamendo.com?type=album&id=391160&width=500",
            genre = "Synthwave",
            source = "Jamendo CC"
        )
    )

    // --- Device Local Music Query ---
    suspend fun getDeviceAudioTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    tracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = contentUri.toString(),
                            artworkUri = artworkUri,
                            isLocal = true,
                            genre = "Local Audio",
                            source = "Device Storage"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    // --- Online Music Catalogue (Jamendo API with CC fallback) ---
    suspend fun getFeaturedOnlineTracks(tag: String? = "electronic"): List<Track> = withContext(Dispatchers.IO) {
        try {
            val response = jamendoApi.getTracks(
                limit = 25,
                tags = tag,
                order = "popularity_week"
            )
            val mapped = response.results.filter { !it.audio.isNullOrBlank() }.map { dto ->
                Track(
                    id = "jamendo_${dto.id}",
                    title = dto.name,
                    artist = dto.artistName,
                    album = dto.albumName ?: "Single",
                    durationMs = (dto.duration ?: 180) * 1000L,
                    mediaUri = dto.audio ?: "",
                    artworkUri = dto.image ?: "",
                    isLocal = false,
                    genre = tag?.replaceFirstChar { it.uppercase() } ?: "Electronic",
                    source = "Jamendo CC"
                )
            }
            if (mapped.isNotEmpty()) mapped else curatedCCTracks
        } catch (e: Exception) {
            // Safe fallback to verified CC streaming tracks
            curatedCCTracks
        }
    }

    suspend fun searchOnlineTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = jamendoApi.getTracks(
                query = query.trim(),
                limit = 30
            )
            val mapped = response.results.filter { !it.audio.isNullOrBlank() }.map { dto ->
                Track(
                    id = "jamendo_${dto.id}",
                    title = dto.name,
                    artist = dto.artistName,
                    album = dto.albumName ?: "Single",
                    durationMs = (dto.duration ?: 180) * 1000L,
                    mediaUri = dto.audio ?: "",
                    artworkUri = dto.image ?: "",
                    isLocal = false,
                    genre = "Creative Commons",
                    source = "Jamendo CC"
                )
            }
            if (mapped.isNotEmpty()) mapped else {
                curatedCCTracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            curatedCCTracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }

    // --- Favorites ---
    fun getAllFavorites(): Flow<List<Track>> {
        return dao.getAllFavorites().map { entities ->
            entities.map { it.toTrack() }
        }
    }

    fun isFavorite(trackId: String): Flow<Boolean> = dao.isFavorite(trackId)

    suspend fun toggleFavorite(track: Track) = withContext(Dispatchers.IO) {
        val isFav = dao.isFavoriteDirect(track.id)
        if (isFav) {
            dao.deleteFavorite(track.id)
        } else {
            dao.insertFavorite(
                FavoriteEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    mediaUri = track.mediaUri,
                    artworkUri = track.artworkUri,
                    isLocal = track.isLocal
                )
            )
        }
    }

    // --- Playlists ---
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return dao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    coverGradientStart = entity.coverGradientStart,
                    coverGradientEnd = entity.coverGradientEnd,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        val colors = listOf(
            0xFFFF4FA3L to 0xFF984FFFL,
            0xFFFF3366L to 0xFFFF80BFL,
            0xFF8A2BE2L to 0xFFFF4FA3L,
            0xFFFF007FL to 0xFF38E5FFL
        )
        val selectedColor = colors.random()
        dao.insertPlaylist(
            PlaylistEntity(
                name = name,
                description = description,
                coverGradientStart = selectedColor.first,
                coverGradientEnd = selectedColor.second
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
        dao.deletePlaylistTracks(playlistId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return dao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toTrack() }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        dao.insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                mediaUri = track.mediaUri,
                artworkUri = track.artworkUri,
                isLocal = track.isLocal,
                orderIndex = (System.currentTimeMillis() % 10000).toInt()
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // --- Playback History ---
    fun getRecentHistory(): Flow<List<Track>> {
        return dao.getRecentHistory().map { entities ->
            entities.map { it.toTrack() }
        }
    }

    suspend fun recordHistory(track: Track) = withContext(Dispatchers.IO) {
        dao.recordHistory(
            HistoryEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                mediaUri = track.mediaUri,
                artworkUri = track.artworkUri,
                isLocal = track.isLocal,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Create initial default playlists if none exist
        val initialPlaylist = PlaylistEntity(
            id = 1,
            name = "Neon Cyberwave",
            description = "High energy synthwave & futuristic pink beats",
            coverGradientStart = 0xFFFF4FA3L,
            coverGradientEnd = 0xFF984FFFL
        )
        dao.insertPlaylist(initialPlaylist)
        // Add default curated tracks
        curatedCCTracks.take(3).forEachIndexed { index, track ->
            dao.insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = 1,
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    mediaUri = track.mediaUri,
                    artworkUri = track.artworkUri,
                    isLocal = track.isLocal,
                    orderIndex = index
                )
            )
        }
    }
}

private fun FavoriteEntity.toTrack() = Track(
    id = trackId,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
    isLocal = isLocal,
    isFavorite = true,
    source = if (isLocal) "Device" else "Online"
)

private fun PlaylistTrackEntity.toTrack() = Track(
    id = trackId,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
    isLocal = isLocal,
    source = if (isLocal) "Device" else "Online"
)

private fun HistoryEntity.toTrack() = Track(
    id = trackId,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
    isLocal = isLocal,
    source = if (isLocal) "Device" else "Online"
)
