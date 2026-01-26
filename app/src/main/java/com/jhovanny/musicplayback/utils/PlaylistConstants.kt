package com.jhovanny.musicplayback.utils

/**
 * A central object for storing constant values related to special, system-managed playlists.
 *
 * Using a constants object like this helps prevent "magic strings" or "magic numbers"
 * scattered throughout the codebase. It ensures that references to special playlists
 * (like "Favorites" or "Recents") are consistent across the app, from the database
 * layer to the UI.
 */
object PlaylistConstants {
    /**
     * The unique, hardcoded ID for the "Favorites" playlist in the database.
     */
    const val ID_FAVORITES = 1L

    /**
     * The unique, hardcoded ID for the "Recents" playlist concept.
     * Note: "Recents" is often a dynamic list and may not have a persistent database entry
     * in the same way as other playlists, but a constant ID can still be useful for UI logic.
     */
    const val ID_RECENT = 2L

    /**
     * The display and database name for the "Favorites" playlist.
     */
    const val NAME_FAVORITES = "Favoritas"

    /**
     * The display name for the "Recents" playlist.
     */
    const val NAME_RECENT = "Recientes"
}
