package com.jhovanny.musicplayback.ui.screens.libraryScreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.ui.components.TopSongsRow
import com.jhovanny.musicplayback.ui.screens.libraryScreen.components.PlaylistRowComponent
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
import com.jhovanny.musicplayback.ui.theme.TextWhite
import com.jhovanny.musicplayback.utils.PlaylistConstants
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import kotlin.math.absoluteValue


/**
 * `LibraryScreen` is the main screen of the music library. It serves as a central hub for users
 * to discover and access their music.
 *
 * It displays several dynamic sections:
 * - A prominent "Top Played" carousel with interactive animations.
 * - Horizontal rows for special playlists like "Favorites" and "Recents".
 * - A list of all user-created playlists.
 *
 * The screen adapts its layout based on whether the mini-player is visible and handles
 * entering/exiting a "selection mode".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit,
) {
    // --- STATE AND DATA OBSERVATION ---

    // Observe various states from the ViewModel. `collectAsState` ensures the UI
    // recomposes automatically when these values change.
    val topSongs by viewModel.topPlayedSongs.collectAsState(initial = emptyList())
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlayerVisible = currentSong != null
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // Playlist-specific constants for easy access.
    val NAME_FAVORITES = PlaylistConstants.NAME_FAVORITES
    val NAME_RECENTS = PlaylistConstants.NAME_RECENT
    val ID_FAVORITES = PlaylistConstants.ID_FAVORITES
    val ID_RECENT = PlaylistConstants.ID_RECENT

    // Observe song lists for special and user-created playlists.
    val favoriteSongs by viewModel.getSongsForPlaylistFlow(ID_FAVORITES).collectAsState(initial = emptyList())
    val recentSongs by viewModel.recentSongs.collectAsState(initial = emptyList())
    val userPlaylists by viewModel.userPlaylistsStats.collectAsState(initial = emptyList())


    // --- UI STATE AND BACK HANDLING ---

    var lastClickTime by remember { mutableLongStateOf(0L) } // Prevents rapid double-clicks on titles.
    val scrollState = rememberScrollState() // Manages the vertical scroll position of the main column.

    // Intercept the back button press only when in selection mode to exit it.
    BackHandler(enabled = isSelectionMode) {
        viewModel.setSelectionMode(false)
    }

    // --- SCREEN LAYOUT & STRUCTURE ---

    Scaffold(
        containerColor = Color.Transparent, // Makes scaffold background see-through.
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
                // Conditionally add padding to the bottom to avoid overlapping with system navigation bars
                // only when the mini-player is not visible.
                .then(
                    if (!isPlayerVisible) {
                        Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    } else {
                        Modifier.padding(bottom = 16.dp)
                    }
                )
        ) {
            // This inner column is scrollable and holds all the content.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up all available vertical space.
                    .verticalScroll(scrollState)
            ) {
                // The main content is only shown if not in selection mode and if there are top songs to display.
                if (!isSelectionMode && topSongs.isNotEmpty()) {

                    // --- "TOP PLAYED" SECTION CAROUSEL ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Most played songs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                // Shadow ensures text readability against a video or complex background.
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            color = TextWhite,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // State for the HorizontalPager that controls the current page and scroll behavior.
                        val pagerState = rememberPagerState(
                            initialPage = if (topSongs.size > 1) 1 else 0,
                            pageCount = { topSongs.size }
                        )

                        // A horizontally scrolling pager that displays items with a carousel effect.
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 80.dp), // Creates the "peek-a-boo" effect for adjacent items.
                            pageSpacing = 16.dp, // Space between pages.
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) { page ->
                            val song = topSongs[page]

                            // --- CAROUSEL ITEM ANIMATION ---
                            // Calculate the offset of the page from the center.
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

                            // Apply scaling and alpha animations based on the item's distance from the center.
                            val scale = lerp(1.15f, 0.85f, absOffset) // Center item is larger.
                            val alpha = lerp(1f, 0.6f, absOffset) // Side items are more transparent.

                            // The card for a single song in the carousel.
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(170.dp)
                                    .graphicsLayer { // Apply transformations.
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .clickable {
                                        // Play the song only if it's the currently centered item.
                                        if (pagerState.currentPage == page) {
                                            viewModel.playSongsFromPlaylist(topSongs, page)
                                            onNavigateToPlayer()
                                        }
                                    }
                            ) {
                                // --- SONG CARD VISUALS ---
                                Box(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .height(130.dp)
                                        // Apply a colored "glow" effect only to the centered item.
                                        .then(
                                            if (absOffset < 0.1f) {
                                                Modifier.shadow(
                                                    elevation = 20.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    spotColor = TextHighlightPurple,
                                                    ambientColor = TextHighlightPurple
                                                )
                                            } else {
                                                Modifier.shadow(
                                                    elevation = 8.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    spotColor = Color.Black
                                                )
                                            }
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black)
                                        // Apply a bright border only to the centered item.
                                        .then(
                                            if (absOffset < 0.1f) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    // Asynchronously load the song's cover art.
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.coverUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Subtle gradient overlay to enhance visual depth.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.3f)
                                                    )
                                                )
                                            )
                                    )

                                    // Display a play icon overlay only on the centered item.
                                    if (absOffset < 0.1f) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Song title and artist, visible only when the item is near the center.
                                AnimatedVisibility(
                                    visible = absOffset < 0.5f,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                shadow = Shadow(
                                                    color = Color.Black,
                                                    offset = Offset(1f, 1f),
                                                    blurRadius = 2f
                                                )
                                            ),
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = song.artist ?: "Desconocido",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                shadow = Shadow(
                                                    color = Color.Black,
                                                    offset = Offset(1f, 1f),
                                                    blurRadius = 2f
                                                )
                                            ),
                                            color = TextWhite.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- "FAVORITES" PLAYLIST ROW ---
                    // This section is only displayed if the user has favorite songs.
                    if (favoriteSongs.isNotEmpty()) {
                        TopSongsRow(
                            name = "Favorites",
                            songs = favoriteSongs,
                            onSongClick = { clickedSong ->
                                val index = favoriteSongs.indexOfFirst { it.id == clickedSong.id }
                                if (index != -1) {
                                    viewModel.playSongsFromPlaylist(favoriteSongs, index)
                                }
                            },
                            onTitleClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) {
                                    lastClickTime = currentTime
                                    onNavigateToDetail(ID_FAVORITES, NAME_FAVORITES)
                                }
                            }
                        )
                        // Separador más pequeño y limpio
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // --- "RECENTS" PLAYLIST ROW ---
                    // This section is only displayed if there are recently played songs.
                    if (recentSongs.isNotEmpty()) {
                        TopSongsRow(
                            name = "Recents",
                            songs = recentSongs,
                            onSongClick = { clickedSong ->
                                val index = recentSongs.indexOfFirst { it.id == clickedSong.id }
                                if (index != -1) {
                                    viewModel.playSongsFromPlaylist(recentSongs, index)
                                }
                            },
                            onTitleClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) {
                                    lastClickTime = currentTime
                                    onNavigateToDetail(ID_RECENT, NAME_RECENTS)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- USER-CREATED PLAYLISTS SECTION ---
                    // Iterate through the user's custom playlists and render a row for each one.
                    userPlaylists.forEach { playlist ->
                        PlaylistRowComponent(
                            viewModel = viewModel,
                            playlistId = playlist.id,
                            playlistName = playlist.name,
                            onNavigateToPlayer = onNavigateToPlayer,
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}
