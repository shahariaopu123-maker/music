package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.TrackItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Device Audio", "History", "All Online")

    val localTracks by viewModel.localTracks.collectAsState()
    val playbackHistory by viewModel.playbackHistory.collectAsState()
    val featuredTracks by viewModel.featuredTracks.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val targetPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, targetPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        viewModel.setStoragePermissionGranted(isGranted)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadDeviceAudioTracks()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "Your Library",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x18FFFFFF))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonPink else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp)
                        .testTag("library_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) ObsidianBlack else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // Device Audio Tab
                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonPink,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Device Audio Access",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pink Wave requires permission to read your on-device audio files so you can play your local music offline.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(targetPermission) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPink,
                                        contentColor = ObsidianBlack
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("grant_permission_btn")
                                ) {
                                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (localTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = DeepPinkMuted,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Local Audio Files Found",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Put MP3/AAC audio files on your device storage to listen offline.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            IconButton(
                                onClick = { viewModel.loadDeviceAudioTracks() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1CFFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = NeonPink
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${localTracks.size} local tracks on device",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = { viewModel.loadDeviceAudioTracks() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh local tracks",
                                        tint = NeonPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        items(localTracks) { track ->
                            val isCurrent = playbackState.currentTrack?.id == track.id
                            val isFav = favorites.any { it.id == track.id }

                            TrackItem(
                                track = track,
                                isPlaying = playbackState.isPlaying,
                                isCurrentTrack = isCurrent,
                                isFavorite = isFav,
                                onClick = { viewModel.playTrack(track, localTracks) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                                onAddToQueue = { viewModel.addToQueue(track) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            1 -> {
                // Playback History Tab
                if (playbackHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tracks played yet",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(playbackHistory) { track ->
                            val isCurrent = playbackState.currentTrack?.id == track.id
                            val isFav = favorites.any { it.id == track.id }

                            TrackItem(
                                track = track,
                                isPlaying = playbackState.isPlaying,
                                isCurrentTrack = isCurrent,
                                isFavorite = isFav,
                                onClick = { viewModel.playTrack(track, playbackHistory) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                                onAddToQueue = { viewModel.addToQueue(track) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            2 -> {
                // All Online Tracks
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
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
        }
    }
}
