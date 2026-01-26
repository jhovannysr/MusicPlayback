package com.jhovanny.musicplayback

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.ui.components.VideoBackground
import com.jhovanny.musicplayback.ui.screens.addSongsScreen.AddSongsScreen
import com.jhovanny.musicplayback.ui.screens.libraryScreen.LibraryScreen
import com.jhovanny.musicplayback.ui.screens.mainScreen.MainScreen
import com.jhovanny.musicplayback.ui.screens.musicListScreen.MusicListScreen
import com.jhovanny.musicplayback.ui.screens.playerScreen.PlayerScreen
import com.jhovanny.musicplayback.ui.screens.playlistDetailScreen.PlaylistDetailScreen
import com.jhovanny.musicplayback.ui.screens.searchScreen.SearchScreen
import com.jhovanny.musicplayback.ui.screensimport.PlayListsScreen
import com.jhovanny.musicplayback.ui.theme.MusicPlaybackTheme
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel

/**
 * The main and only activity in the application, serving as the entry point.
 *
 * This activity is responsible for:
 * 1. Setting up the edge-to-edge UI.
 * 2. Handling runtime permissions for reading audio files and showing notifications.
 * 3. Initializing the shared [PlayerViewModel] and the Media3 controller.
 * 4. Setting up the Jetpack Compose navigation graph (`NavHost`).
 * 5. Managing a global background that changes based on the current navigation route.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_MusicPlayback)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MusicPlaybackTheme {
                val context = LocalContext.current
                val viewModel: PlayerViewModel = viewModel()
                val navController = rememberNavController()

                // Observe the current navigation route to control UI elements, like the background.
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // The background should be darkened on all screens except the main player screen.
                val shouldDarken = currentRoute != "player"

                // Set up the activity result launcher to handle permission request outcomes.
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val readPermissionGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
                        } else {
                            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                        }

                    if (readPermissionGranted) {
                        viewModel.loadAudioFiles()
                    }
                }

                // This effect runs once when the app starts.
                LaunchedEffect(Unit) {
                    // Initialize the connection to the Media3 playback service.
                    viewModel.initController()

                    // Check for and request necessary runtime permissions based on the Android version.
                    val permissionToCheck =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                    if (ContextCompat.checkSelfPermission(
                            context,
                            permissionToCheck
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // If permission is already granted, load the audio files immediately.
                        viewModel.loadAudioFiles()
                    } else {
                        // Otherwise, build a list of permissions to request.
                        val permissionsToRequest = mutableListOf(permissionToCheck)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                // Global layout container.
                Box(modifier = Modifier.fillMaxSize()) {
                    // The video background is a fixed layer for the entire app.
                    VideoBackground(
                        videoResId = R.raw.background,
                        shouldDarken = shouldDarken
                    )

                    // The NavHost sits on top of the background, managing all screen transitions.
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") },
                                onNavigateToSearch = { navController.navigate("search_screen") },
                                onNavigateToPlaylistDetail = { id, name ->
                                    navController.navigate("playlist_detail/$id/$name")
                                },
                                onNavigateToAddSongs = { id ->
                                    navController.navigate("add_songs/$id")
                                }
                            )
                        }

                        composable(
                            route = "player",
                            enterTransition = { slideInVertically(initialOffsetY = { it }) },
                            exitTransition = { slideOutVertically(targetOffsetY = { it }) }
                        ) {
                            PlayerScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("playlists") {
                            PlayListsScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, name ->
                                    navController.navigate("playlist_detail/$id/$name")
                                },
                                onNavigateToAddSongs = { id ->
                                    navController.navigate("add_songs/$id")
                                }
                            )
                        }

                        composable(
                            route = "playlist_detail/{playlistId}/{playlistName}",
                            arguments = listOf(
                                navArgument("playlistId") { type = NavType.LongType },
                                navArgument("playlistName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                            val name = backStackEntry.arguments?.getString("playlistName") ?: ""
                            PlaylistDetailScreen(
                                playlistId = id,
                                playlistName = name,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToAddSongs = { playlistId ->
                                    navController.navigate("add_songs/$playlistId")
                                },
                                onDeletePlaylist = {
                                    val entity = PlaylistEntity(id = id, name = name)
                                    viewModel.deletePlaylist(entity)
                                    navController.popBackStack()
                                },
                                onNavigateToPlayer = { navController.navigate("player") }
                            )
                        }

                        composable(
                            "add_songs/{playlistId}",
                            arguments = listOf(navArgument("playlistId") {
                                type = NavType.LongType
                            })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                            AddSongsScreen(
                                playlistId = id,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onSongsAdded = { navController.popBackStack() }
                            )
                        }

                        composable("library_route") {
                            LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") },
                                onNavigateToDetail = { id, name ->
                                    navController.navigate("playlist_detail/$id/$name")
                                }
                            )
                        }

                        composable("music_list_route") {
                            MusicListScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") },
                                onNavigateToSearch = { navController.navigate("search_screen") }
                            )
                        }

                        composable("search_screen") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPlayer = { navController.navigate("player") }
                            )
                        }
                    }
                }
            }
        }
    }
}