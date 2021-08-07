package com.akhilasdeveloper.marsroverphotos.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import java.util.*

@Dao
interface MarsRoverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedDate(scannedDatesDb: ScannedDatesDb)

    @Query("DELETE FROM scanned_dates_table")
    suspend fun deleteScannedDate()

    @Query("SELECT * FROM mars_rover_photo_table WHERE rover_id = :roverID ORDER BY earth_date DESC,id DESC LIMIT (:page - 1) * :size,:size")
    fun getPhotosByRoverID(roverID: Int, page: Int, size: Int = 25): List<MarsRoverPhotoDb>

    @Query("SELECT max(id) FROM mars_rover_photo_table WHERE earth_date = :date and rover_id = :roverID")
    fun getIDForDate(date: Long, roverID: Int): Int

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE earth_date = :date")
    fun isPhotosByDateExist(date: Long): Int

    @Query("SELECT max(earth_date) FROM mars_rover_photo_table")
    fun latestDate(): Long?

    @Query("SELECT max(date) FROM scanned_dates_table")
    fun latestScannedDate(): Long?

    @Query("SELECT count(*) FROM scanned_dates_table WHERE date = :date")
    fun isLatestScannedDate(date: Long): Int

}
