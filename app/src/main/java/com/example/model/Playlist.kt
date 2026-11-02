package com.example.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverGradientStart: Long = 0xFFFF4FA3,
    val coverGradientEnd: Long = 0xFF984FFF,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
