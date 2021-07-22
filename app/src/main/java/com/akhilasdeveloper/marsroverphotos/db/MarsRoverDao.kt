package com.akhilasdeveloper.marsroverphotos.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MarsRoverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb)

    @Query("SELECT * FROM mars_rover_photo_table WHERE earth_date = :date")
    fun getPhotosByDate(date: String): PagingSource<Int,MarsRoverPhotoDb>


    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE earth_date = :date")
    fun isPhotosByDateExist(date: String): Int

}
