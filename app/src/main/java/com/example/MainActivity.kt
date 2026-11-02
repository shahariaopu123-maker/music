package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Playlist
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PinkWaveTheme {
                // Request POST_NOTIFICATIONS permission on Android 13+ for media controls in notification
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled by OS */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val viewModel: MusicViewModel = viewModel()
                PinkWaveApp(viewModel)
            }
        }
    }
}

@Composable
fun PinkWaveApp(viewModel: MusicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val trackToAddToPlaylist by viewModel.trackToAddToPlaylist.collectAsState()

    // Handle back button smoothly
    BackHandler(enabled = isNowPlayingExpanded || currentScreen !is Screen.Home) {
        if (isNowPlayingExpanded) {
            viewModel.setNowPlayingExpanded(false)
        } else if (currentScreen is Screen.PlaylistDetail) {
            viewModel.navigateTo(Screen.Playlists)
        } else {
            viewModel.navigateTo(Screen.Home)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        containerColor = ObsidianBlack,
        bottomBar = {
            if (!isNowPlayingExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Floating MiniPlayer docked directly above the navigation bar
                    AnimatedVisibility(
                        visible = playbackState.currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onExpandNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipToNext() }
                        )
                    }

                    // Liquid Glass Navigation Bar
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = GlassBorder,
                                shape = androidx.compose.ui.graphics.RectangleShape
                            )
                            .testTag("pink_wave_bottom_nav")
                    ) {
                        val navItems = listOf(
                            Triple(Screen.Home, "Home", Icons.Default.Home),
                            Triple(Screen.Search, "Search", Icons.Default.Search),
                            Triple(Screen.Playlists, "Playlists", Icons.Default.QueueMusic),
                            Triple(Screen.Favorites, "Favorites", Icons.Default.Favorite),
                            Triple(Screen.Library, "Library", Icons.Default.Folder)
                        )

                        navItems.forEach { (screen, label, icon) ->
                            val isSelected = currentScreen::class == screen::class
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) NeonPink else TextMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = NeonPink,
                                    indicatorColor = NeonPink,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_item_$label")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Main Screen Switcher
            AnimatedContent(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(viewModel = viewModel)
                    is Screen.Search -> SearchScreen(viewModel = viewModel)
                    is Screen.Playlists -> PlaylistsScreen(viewModel = viewModel)
                    is Screen.Favorites -> FavoritesScreen(viewModel = viewModel)
                    is Screen.Library -> LibraryScreen(viewModel = viewModel)
                    is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    is Screen.PlaylistDetail -> PlaylistDetailScreen(
                        playlist = screen.playlist,
                        viewModel = viewModel
                    )
                }
            }

            // Fullscreen Now Playing Modal Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setNowPlayingExpanded(false) }
                )
            }

            // Add to Playlist Dialog
            trackToAddToPlaylist?.let { track ->
                AddToPlaylistDialog(
                    track = track,
                    playlists = playlists,
                    onDismiss = { viewModel.dismissAddToPlaylistDialog() },
                    onAddToPlaylist = { playlistId ->
                        viewModel.addTrackToPlaylist(playlistId, track)
                    },
                    onCreateNewPlaylist = { name ->
                        viewModel.createPlaylist(name)
                    }
                )
            }
        }
    }
}
