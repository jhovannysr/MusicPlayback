package com.jhovanny.musicplayback.ui.screens.mainScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.ui.components.MiniPlayer
import com.jhovanny.musicplayback.ui.screens.libraryScreen.LibraryScreen
import com.jhovanny.musicplayback.ui.screens.musicListScreen.MusicListScreen
import com.jhovanny.musicplayback.ui.screensimport.PlayListsScreen
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
/**
* The main screen of the application, which serves as a container for the primary
* navigation tabs and the mini-player.
*
* This screen uses a [Scaffold] and a [HorizontalPager] to create a tabbed layout
* for "Library", "Songs", and "Playlists". It also conditionally displays a contextual
* [TopAppBar] when in selection mode and a [MiniPlayer] at the bottom if a song is active.
*
* @param viewModel The shared [PlayerViewModel] that provides state and handles business logic.
* @param onNavigateToPlayer A callback to navigate to the full-screen player UI.
* @param onNavigateToSearch A callback to navigate to the search screen.
* @param onNavigateToPlaylistDetail A callback to navigate to the detail screen for a specific playlist.
* @param onNavigateToAddSongs A callback to navigate to the screen for adding songs to a playlist.
*/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylistDetail: (Long, String) -> Unit,
    onNavigateToAddSongs: (Long) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val titles = listOf("Library", "Songs", "Playlists")

    // Observe player state from the ViewModel to control the MiniPlayer.
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val showMiniPlayer = currentSong != null

    // Observe selection mode state to control the TopAppBar.
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()

    // Using a Box with a Scaffold allows the MiniPlayer to correctly overlay the main content.
    Box(modifier = Modifier.fillMaxSize())
    Scaffold(
        // The container is transparent to allow the global video background to be visible.
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isSelectionMode) {
                // Default TopBar with navigation tabs.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding() // Add padding to avoid overlapping with system status bar.
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = IconPrimary,
                        modifier = Modifier.height(40.dp),
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                                color = MaterialTheme.colorScheme.onSurface,
                                width = 30.dp,
                                height = 3.dp,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                        },
                        divider = {} // Remove the default divider line.
                    ) {
                        titles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.height(40.dp),
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                // Contextual TopAppBar for selection mode.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                ) {
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        TopAppBar(
                            modifier = Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            ),
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            title = {
                                Text(
                                    text = "${selectedSongIds.size} selected",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.setSelectionMode(false) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancel Selection"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.setShowBottomSheet(true) }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More Options"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // The MiniPlayer is displayed only if there is an active song.
            if (showMiniPlayer && currentSong != null) {
                MiniPlayer(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onPrev = { viewModel.skipPrevious() },
                    onClick = onNavigateToPlayer
                )
            }
        }
    ) { innerPadding ->
        // The HorizontalPager manages the content for each tab.
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2, // Keep adjacent pages loaded for smooth swiping.
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            // Reset selection mode when swiping between pages.
            viewModel.setSelectionMode(false)
            when (page) {
                0 -> LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToDetail = onNavigateToPlaylistDetail,
                )

                1 -> MusicListScreen(
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToSearch = onNavigateToSearch
                )

                2 -> PlayListsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToPlaylistDetail,
                    onNavigateToAddSongs = onNavigateToAddSongs
                )
            }
        }
    }
}