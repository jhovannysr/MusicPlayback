package com.jhovanny.musicplayback.ui.screens.playerScreen

import android.R
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.components.AddToPlaylistDialog
import com.jhovanny.musicplayback.ui.screens.musicListScreen.components.MenuOptionItemMinimal
import com.jhovanny.musicplayback.ui.theme.IconError
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
import com.jhovanny.musicplayback.ui.theme.TextWarning
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * The full-screen player UI, which provides an immersive playback experience.
 *
 * This screen is responsible for:
 * - Displaying the current song's cover art via a swipeable [HorizontalPager].
 * - Synchronizing the Pager with the player state, handling song changes, reordered lists (shuffle), etc.
 * - Providing playback controls (play/pause, next/previous, shuffle, repeat).
 * - Displaying song information and a seekable progress bar.
 * - Offering additional features like "favorites," an options menu, and a UI "lock" mode.
 * - Managing the visibility of UI elements through timers and user taps.
 *
 * @param viewModel The shared [PlayerViewModel] that provides all player state and handles business logic.
 * @param onBack A callback to navigate back from the player screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Launcher to handle the system's confirmation dialog when deleting files.
    // This is required to comply with Scoped Storage policies.
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed the deletion. The ViewModel handles the rest.
            viewModel.onSongsDeletedSuccess()
        } else {
            // User cancelled the operation. Exit selection mode.
            viewModel.setSelectionMode(false)
        }
    }

    // --- State Observation from ViewModel ---
    // Collect all necessary states to drive the UI.
    val currentSong by viewModel.currentSong.collectAsState()
    val songList by viewModel.songList.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentSongFavorite.collectAsState()

    // Calculate the initial page for the Pager based on the current song.
    val initialIndex = remember(songList, currentSong) {
        if (currentSong != null) songList.indexOfFirst { it.id == currentSong!!.id }
            .coerceAtLeast(0) else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { songList.size }
    )

    // Control variable to prevent update loops between the Pager and the ViewModel.
    var lastHandledPage by remember { mutableIntStateOf(initialIndex) }

    // --- Local UI States ---
    // State for the options menu (BottomSheet).
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    var songForMenuParams by remember { mutableStateOf<Song?>(null) }

    // State for the "Add to Playlist" dialog.
    val savedPlaylists by viewModel.savedPlaylists.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // State for the UI lock mode, which hides controls.
    var isLockedMode by remember { mutableStateOf(false) }

    // State for the cover art visibility, which can be hidden manually or automatically.
    var isCoverVisible by remember { mutableStateOf(true) }

    // --- Synchronization Logic (LaunchedEffects) ---

    // Smart timer to automatically hide the cover art after 15 seconds of inactivity.
    LaunchedEffect(isCoverVisible, currentSong, pagerState.isScrollInProgress, isLockedMode) {
        // Activates only if the cover art is visible, the user isn't scrolling, and the UI isn't locked.
        if (isCoverVisible && !pagerState.isScrollInProgress && !isLockedMode) {
            delay(15000) // 15-second delay.
            isCoverVisible = false // Hide the cover art.
        }
    }

    // Effect to sync the Pager when the song changes externally (e.g., from buttons, notification).
    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            val index = songList.indexOfFirst { it.id == currentSong!!.id }
            if (index != -1 && index != pagerState.currentPage) {
                // Perform an instant jump (no animation) to the new song's page.
                pagerState.scrollToPage(index)
                lastHandledPage = index // Update the handled page to prevent a feedback loop.
            }
        }
    }

    // Effect to sync the Pager when the playlist changes (e.g., when toggling shuffle).
    LaunchedEffect(songList) {
        if (currentSong != null) {
            val newIndex = songList.indexOfFirst { it.id == currentSong!!.id }
            if (newIndex != -1 && newIndex != pagerState.currentPage) {
                // Jump immediately to the new position so the cover art doesn't visually change.
                pagerState.scrollToPage(newIndex)
                lastHandledPage = newIndex
            }
        }
    }

    // Effect to notify the ViewModel when the user manually swipes to a new song.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage } // Observe only when the Pager settles on a new page.
            .collect { page ->
                // Avoid unnecessary executions if the page hasn't actually changed.
                if (page != lastHandledPage && page in songList.indices) {
                    val songAtPage = songList[page]
                    if (songAtPage.id != currentSong?.id) {
                        // Notify the ViewModel to skip to this song.
                        viewModel.skipToSong(songAtPage)
                    }
                    lastHandledPage = page // Update the handled page.
                }
            }
    }

    // Effect to check if the current song is a favorite every time it changes.
    LaunchedEffect(currentSong?.id) {
        viewModel.checkIsFavorite()
    }

    // Effect to handle what happens if the current song disappears (e.g., it was deleted).
    LaunchedEffect(currentSong) {
        if (currentSong == null) {
            // If there's no song, we can't be on this screen, so navigate back.
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    // This button toggles the UI lock mode, hiding/showing all controls.
                    IconButton(onClick = {
                        isLockedMode = !isLockedMode
                        // When locking, immediately hide the cover art.
                        // When unlocking, show it again.
                        isCoverVisible = !isLockedMode
                    }) {
                        Icon(
                            imageVector = if (isLockedMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Lock UI",
                            tint = Color.White
                        )
                    }
                    // Button to open the options BottomSheet.
                    IconButton(onClick = { viewModel.setShowBottomSheet(true) }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        // This Box acts as a background tap detector to toggle the cover art's visibility.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 250.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isLockedMode) {
                        // Toggle the visibility of the cover art.
                        isCoverVisible = !isCoverVisible
                    }
                }
        )

        // A decorative Box that adds a subtle gradient at the bottom of the screen.
        // This ensures that text and controls remain legible over any background video.
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        // This is the main content Column, containing the cover art and all controls.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // Distribute vertical space nicely.
        ) {
            // This parent Box uses `weight(1f)` to occupy all available vertical space.
            // This is the key to keeping the controls at the bottom fixed, as this Box
            // will not shrink even when its content (the AnimatedVisibility) is hidden.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // The cover art pager is wrapped in AnimatedVisibility to fade in and out.
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCoverVisible,
                    enter = androidx.compose.animation.fadeIn(animationSpec = tween(500)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = tween(500))
                ) {
                    // This inner Box simply centers the pager within the available space.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // The HorizontalPager allows swiping between songs' cover art.
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            // Calculate the offset for parallax and scaling effects.
                            val pageOffset =
                                (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue
                            // Interpolate scale and alpha for a smooth visual transition between pages.
                            val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                            val alphaValue = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (page == pagerState.currentPage) 12.dp else 4.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    // Apply the calculated scale and alpha for a dynamic effect.
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = alphaValue
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(songList.getOrNull(page)?.coverUri)
                                        .crossfade(true)
                                        .size(coil.size.Size.ORIGINAL)
                                        .error(R.drawable.ic_menu_help)
                                        .build(),
                                    contentDescription = "Cover Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // This AnimatedVisibility wraps all the main controls, allowing them to fade in and out together
            // and be hidden when the UI is in "lock mode".
            androidx.compose.animation.AnimatedVisibility(
                visible = !isLockedMode,
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(500)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(500))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Center all children by default.
                ) {
                    // This Row contains the song title/artist and the favorite button.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // The Column for the title and artist uses a weight to take up all available
                        // horizontal space, pushing the favorite button to the end.
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong?.title ?: "Not Playing",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    // A shadow effect ensures the text is readable over a video background.
                                    shadow = Shadow(Color.Black, Offset(2f, 2f), 8f)
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = currentSong?.artist ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    shadow = Shadow(Color.Black, Offset(1f, 1f), 4f)
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // The "Favorite" button, placed at the end of the row.
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) IconError else Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(8.dp, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 3. NEON-STYLE SLIDER ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = TextHighlightPurple, // Custom purple highlight color.
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .height(12.dp)
                                .padding(bottom = 30.dp) // Negative padding trick to make the slider track thinner.
                        )

                        // Row for displaying the current time and total duration labels.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- 4. GLASSY PLAYBACK CONTROLS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, // Use SpaceBetween for wider spacing.
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle Button
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffleEnabled) TextHighlightPurple else Color.White.copy(
                                    alpha = 0.7f
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Previous Button
                        IconButton(
                            onClick = { viewModel.skipPrevious() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // PLAY/PAUSE (Large, stylized central button)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(TextHighlightPurple, IconPrimary)
                                    )
                                )
                                // A subtle border to give a "glassy" effect.
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next Button
                        IconButton(
                            onClick = { viewModel.skipNext() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Repeat Button
                        IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                            // Determine the correct icon and tint based on the current repeat mode.
                            val (icon, tint) = when (repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_OFF -> Pair(
                                    Icons.Default.Repeat,
                                    Color.White.copy(alpha = 0.7f)
                                )

                                androidx.media3.common.Player.REPEAT_MODE_ALL -> Pair(
                                    Icons.Default.Repeat,
                                    TextHighlightPurple
                                )

                                androidx.media3.common.Player.REPEAT_MODE_ONE -> Pair(
                                    Icons.Default.RepeatOne,
                                    TextHighlightPurple
                                )

                                else -> Pair(Icons.Default.Repeat, Color.White.copy(alpha = 0.7f))
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Repeat",
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Display the cover art, title and artist of the current song.
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowBottomSheet(false) },
            sheetState = sheetState,
            containerColor = Color(0xFF121212).copy(alpha = 0.98f),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth() // The column will take the full available width.
                    .padding(10.dp) // Apply 10dp of padding on all sides.
            ) {
                // --- DISPLAY CURRENT SONG INFO OR A GENERIC TITLE ---
                // Check if there's a song currently loaded.
                if (currentSong != null) {
                    // If a song is playing, display its details in a horizontal layout.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp), // Add space below this row.
                        verticalAlignment = Alignment.CenterVertically // Center items vertically within the row.
                    ) {
                        // 1. SMALL ALBUM COVER
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            // Asynchronously load the album art.
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentSong?.coverUri)
                                    .crossfade(true)
                                    .error(R.drawable.ic_menu_help)
                                    .build(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp)) // Add horizontal space between the cover and the text.

                        // 2. SONG TITLE AND ARTIST
                        Column { // Arrange title and artist vertically.
                            Text(
                                text = currentSong?.title
                                    ?: "Unknown", // Display title or "Unknown" if null.
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1, // Restrict to a single line.
                                overflow = TextOverflow.Ellipsis // Add "..." if the text is too long.
                            )
                            Text(
                                text = currentSong?.artist
                                    ?: "Unknown artist", // Display artist or "Unknown artist" if null.
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // If there is no current song available...
                else {
                    // Display a generic "Options" title.
                    Text(
                        text = "Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 20.dp) // Add space below the title.
                    )
                }

                // --- MENU OPTIONS ---

                // Option 1: Add to Playlist
                MenuOptionItemMinimal(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = "Add to playlist",
                    onClick = {
                        viewModel.setShowBottomSheet(false) // Dismiss the bottom sheet.
                        showAddToPlaylistDialog = true // Trigger the "Add to Playlist" dialog.
                    }
                )

                Spacer(modifier = Modifier.height(12.dp)) // Vertical space between menu items.

                // Option 2: Delete Song
                MenuOptionItemMinimal(
                    icon = Icons.Default.Delete,
                    text = "Delete Song",
                    textColor = TextWarning.copy(alpha = 0.8f), // Use a warning color for the text.
                    onClick = {
                        // 1. Verify that there is a song currently playing.
                        val songToDelete = currentSong

                        if (songToDelete != null) {
                            // Create a Set containing the unique ID of the current song.
                            val targetIds = setOf(songToDelete.id)

                            // IMPORTANT: Save this ID in the ViewModel in case the 'deleteLauncher'
                            // needs to know what to delete after requesting permission (Android 10+).
                            viewModel.setSelectedSongIds(targetIds)

                            // 2. CALL THE DELETE FUNCTION IN THE VIEWMODEL
                            viewModel.deleteSongs(
                                context = context,
                                onSuccess = {
                                    // Direct success (Android < 10 or permission already granted).
                                    viewModel.onSongsDeletedSuccess(targetIds.toList())
                                    // Clear the selection after deletion.
                                    viewModel.setSelectedSongIds(emptySet())
                                },
                                onNeedPermission = { intentSender ->
                                    // Android 10+ requires user permission -> Launch the system dialog.
                                    val intentSenderRequest =
                                        IntentSenderRequest.Builder(intentSender).build()
                                    deleteLauncher.launch(intentSenderRequest)
                                },
                                selectedSong = targetIds // Pass the ID of the current song.
                            )
                        }

                        viewModel.setShowBottomSheet(false) // Dismiss the bottom sheet after initiating the action.
                    }
                )
            }
        }
    }

    // --- CENTRAL DIALOG FOR ADDING TO A PLAYLIST ---
    if (showAddToPlaylistDialog) {
        val targetSongId = currentSong?.id

        if (targetSongId != null) {
            AddToPlaylistDialog(
                playlists = savedPlaylists,
                onDismiss = { showAddToPlaylistDialog = false },

                // CASE 1: ADDING TO A CUSTOM PLAYLIST
                onPlaylistSelected = { playlistId ->
                    val idsToAdd = listOf(targetSongId)
                    viewModel.addSongsToPlaylist(playlistId, idsToAdd)

                    Toast.makeText(
                        context,
                        "Added to playlist",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    showAddToPlaylistDialog = false
                },

                // CASE 2: ADDING TO "FAVORITES"
                onFavoritesSelected = {
                    val idsToAdd = listOf(targetSongId)

                    val favoritesPlaylist =
                        savedPlaylists.find { it.name == "Favorites" || it.isSpecial }

                    if (favoritesPlaylist != null) {
                        viewModel.addSongsToPlaylist(favoritesPlaylist.id, idsToAdd)
                    } else {
                        viewModel.toggleFavorite()
                    }

                    showAddToPlaylistDialog = false
                }
            )
        } else {
            // If for some reason there is no current song, just close the dialog.
            showAddToPlaylistDialog = false
        }
    }

}

// Simple auxiliary function for mm:ss format
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}