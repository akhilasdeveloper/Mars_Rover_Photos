package com.akhilasdeveloper.marsroverphotos.db.dao

import androidx.room.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.DisplayPhotosTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.PhotoDatesTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverManifestTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable

@Dao
interface PhotoKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisplayPhotos(displayPhotosTable: DisplayPhotosTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoDate(photoDatesTable: PhotoDatesTable)

    @Query("SELECT * FROM display_photos")
    fun getDisplayPhotos(): List<DisplayPhotosTable>

    @Query("SELECT count(*) FROM photo_dates WHERE roverName = :roverName")
    fun getPhotoDatesCount(roverName: String): Int

    @Query("SELECT count(*) FROM photo_dates WHERE roverName = :roverName AND date = :date")
    fun getPhotoDatesCountByDate(roverName: String, date: Long): Int

    @Query("SELECT count(*) FROM photo_dates WHERE roverName = :roverName AND date > :date")
    fun getAllPhotoDatesCountByDate(roverName: String, date: Long): Int

    @Query("SELECT * FROM photo_dates WHERE roverName = :roverName ORDER BY date DESC LIMIT :pageSize OFFSET :pageOffset")
    fun getPhotoDatesByPage(roverName: String, pageSize: Int, pageOffset: Int): List<PhotoDatesTable>
}
