package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val mediaUri: String,
    val artworkUri: String = "",
    val isLocal: Boolean = false,
    val isFavorite: Boolean = false,
    val genre: String = "Wave",
    val source: String = "Online"
) {
    fun formattedDuration(): String {
        if (durationMs <= 0) return "--:--"
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }
}
