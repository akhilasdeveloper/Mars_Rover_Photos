package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.dao.PhotoKeyDao
import com.akhilasdeveloper.marsroverphotos.db.dao.RemoteKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.DisplayPhotosTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.PhotoDatesTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.RemoteKeysTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverManifestTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable

@Database(
    entities = [MarsRoverPhotoTable::class,
        MarsRoverSrcTable::class,
        MarsRoverManifestTable::class,
        MarsRoverPhotoLikedTable::class,
        RemoteKeysTable::class,
        DisplayPhotosTable::class,
        PhotoDatesTable::class],
    version = 2
)
abstract class MarsRoverDatabase : RoomDatabase() {
    abstract fun getMarsPhotoDao(): MarsPhotoDao
    abstract fun getMarsRoverDao(): MarsRoverDao
    abstract fun getRemoteKeysDao(): RemoteKeyDao
    abstract fun getPhotoKeyDao(): PhotoKeyDao
}