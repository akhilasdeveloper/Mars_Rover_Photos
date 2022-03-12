package com.akhilasdeveloper.marsroverphotos.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable

@Dao
interface MarsPhotoDao {

    /**
     * marsRoverPhotoDb
     */

    @Query("SELECT * FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName ORDER BY earth_date DESC")
    fun getDisplayPhotosByRoverNameAndDate(roverName: String, date: Long): List<MarsRoverPhotoTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable)

    @Delete
    fun removeLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable)

    @Query("SELECT count(*) FROM mars_rover_photo_liked_table WHERE id = :id")
    fun isLiked(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMarsRoverPhotos(users: List<MarsRoverPhotoTable>)

    @Query("SELECT * FROM mars_rover_photo_table WHERE photo_id IN (SELECT id FROM mars_rover_photo_liked_table WHERE rover_id = :roverID) ORDER BY earth_date DESC")
    fun getSavedPhotos(roverID: Int): PagingSource<Int, MarsRoverPhotoTable>


}
