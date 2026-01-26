package com.jhovanny.musicplayback.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jhovanny.musicplayback.data.local.dao.PlaylistDao
import com.jhovanny.musicplayback.data.local.dao.SongStatsDao
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.data.local.entities.PlaylistSongCrossRef
import com.jhovanny.musicplayback.data.local.entities.SongStatsEntity

@Database(
    entities = [PlaylistEntity::class,
    PlaylistSongCrossRef::class,
    SongStatsEntity::class],
            version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun songStatsDao(): SongStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
