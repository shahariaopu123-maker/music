package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.TrackItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val featuredTracks by viewModel.featuredTracks.collectAsState()
    val isLoadingFeatured by viewModel.isLoadingFeatured.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val genres = listOf("Electronic", "Synthwave", "Chillout", "Lo-Fi", "Ambient", "Dance", "Pop")
    var selectedGenre by remember { mutableStateOf("Electronic") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PINK WAVE",
                        color = NeonPink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Liquid Audio Engine",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Search) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassBorder, CircleShape)
                            .testTag("home_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassBorder, CircleShape)
                            .testTag("home_library_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Library",
                            tint = NeonPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassBorder, CircleShape)
                            .testTag("home_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Hero Banner Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("home_hero_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                // Banner background image
                Image(
                    painter = painterResource(id = R.drawable.img_neon_hero),
                    contentDescription = "Featured neon hero banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Translucent gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x2208080C),
                                    Color(0xCC08080C)
                                )
                            )
                        )
                )

                // Content inside Hero
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonPink)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FEATURED",
                                color = ObsidianBlack,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Creative Commons • Legal Stream",
                            color = SoftPink,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Neon Cyberwave Sessions",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (featuredTracks.isNotEmpty()) {
                                viewModel.playTrack(featuredTracks.first(), featuredTracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("hero_play_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play Wave",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Genre filter pills
        item {
            Text(
                text = "Explore Vibes",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) NeonPink else Color(0x18FFFFFF))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonPink else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedGenre = genre
                                viewModel.loadFeaturedOnlineTracks(genre.lowercase())
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("genre_pill_$genre")
                    ) {
                        Text(
                            text = genre,
                            color = if (isSelected) ObsidianBlack else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section Title: Wave Hits
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedGenre Wave",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isLoadingFeatured) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NeonPink,
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // List of tracks
        items(featuredTracks) { track ->
            val isCurrent = playbackState.currentTrack?.id == track.id
            val isFav = favorites.any { it.id == track.id }

            TrackItem(
                track = track,
                isPlaying = playbackState.isPlaying,
                isCurrentTrack = isCurrent,
                isFavorite = isFav,
                onClick = { viewModel.playTrack(track, featuredTracks) },
                onFavoriteToggle = { viewModel.toggleFavorite(track) },
                onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                onAddToQueue = { viewModel.addToQueue(track) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
