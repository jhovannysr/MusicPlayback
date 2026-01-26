package com.jhovanny.musicplayback.data.local.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Represents the join table for the many-to-many relationship between playlists and songs.
 *
 * This entity, often called a "cross-reference" table, links a `PlaylistEntity` to a `Song`
 * by their respective IDs. It also stores metadata about the relationship itself, such as
 * the order of the song within the playlist.
 *
 * The `indices` are defined to speed up queries that filter by either `playlistId` or `songId`.
 *
 * @property playlistId The ID of the playlist. Part of the composite primary key.
 * @property songId The ID of the song. Part of the composite primary key.
 * @property addedTimestamp The timestamp (in milliseconds) when the song was added to the playlist. Defaults to the current time.
 * @property orderIndex An integer used to maintain a custom, user-defined order of songs within a playlist. Defaults to 0.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)