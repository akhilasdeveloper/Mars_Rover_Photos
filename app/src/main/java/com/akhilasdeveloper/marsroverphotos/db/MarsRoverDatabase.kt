package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MarsRoverPhotoDb::class],
    version = 1
)
abstract class MarsRoverDatabase : RoomDatabase() {
    abstract fun getMarsRoverDao(): MarsRoverDao
}