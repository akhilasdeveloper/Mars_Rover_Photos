package com.akhilasdeveloper.marsroverphotos.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import java.util.*

@Dao
interface MarsRoverDao {

    /**
     * marsRoverPhotoDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb)

    @Query("SELECT * FROM mars_rover_photo_table WHERE rover_name = :roverName AND earth_date = :date ORDER BY id DESC")
    fun getPhotosByRoverIDAndDate(roverName: String, date: Long): PagingSource<Int, MarsRoverPhotoDb>

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName")
    fun isPhotosByDateExist(date: Long, roverName:String): Int

    @Query("UPDATE mars_rover_photo_table SET liked = :like WHERE id = :id")
    fun updateLike(like: Boolean, id: Int): Int

    @Update
    fun update(marsRoverPhotoDb: MarsRoverPhotoDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMarsRoverPhotos(users: List<MarsRoverPhotoDb>)

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE rover_name = :roverName AND earth_date = :date")
    suspend fun dataCount(roverName: String, date: Long): Int

    @Query("DELETE FROM mars_rover_photo_table")
    suspend fun clearAll()

    @Query("DELETE FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName")
    suspend fun clearByRoverIDAndDate(date: Long, roverName:String)

    /**
     * MarsRoverSrcDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverSrc(marsRoverSrcDb: MarsRoverSrcDb)

    @Query("SELECT * FROM mars_rover_source_table")
    fun getMarsRoverSrc(): List<MarsRoverSrcDb>

    @Query("SELECT addedDate FROM mars_rover_source_table LIMIT 1")
    suspend fun getInsertDate(): Long?

    /**
     * MarsRoverManifestDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverManifestDb(marsRoverManifestDb: MarsRoverManifestDb)

    @Query("SELECT * FROM mars_rover_manifest_table WHERE name = :roverName")
    fun getMarsRoverManifest(roverName: String): MarsRoverManifestDb?
}
