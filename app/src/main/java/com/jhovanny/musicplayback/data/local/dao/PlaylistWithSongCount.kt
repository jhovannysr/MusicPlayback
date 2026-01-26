package com.jhovanny.musicplayback.data.local.dao

import androidx.room.Embedded
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity

/**
 * A data class designed to hold the result of a custom Room query.
 *
 * This class is not an entity itself, but rather a Plain Old Kotlin Object (POKO)
 * used by a DAO to return a `PlaylistEntity` combined with an aggregated value,
 * in this case, the total count of songs in that playlist.
 *
 * @property playlist The full [PlaylistEntity] object. The `@Embedded` annotation
 *                    tells Room to treat all fields of `PlaylistEntity` as if they
 *                    were declared directly in this class.
 * @property songCount The total number of songs associated with the playlist.
 */
data class PlaylistWithSongCount(
    @Embedded val playlist: PlaylistEntity,
    val songCount: Int
)