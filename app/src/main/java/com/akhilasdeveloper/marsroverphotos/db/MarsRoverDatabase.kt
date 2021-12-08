package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MarsRoverPhotoDb::class, MarsRoverSrcDb::class, MarsRoverManifestDb::class, MarsRoverPhotoLikedDb::class, RemoteKeysDb::class],
    version = 2
)
abstract class MarsRoverDatabase : RoomDatabase() {
    abstract fun getMarsRoverDao(): MarsRoverDao
    abstract fun getRemoteKeysDao(): RemoteKeyDao
}