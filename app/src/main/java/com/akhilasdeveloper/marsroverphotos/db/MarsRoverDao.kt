package com.akhilasdeveloper.marsroverphotos.db

import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface MarsRoverDao {

    /**
     * marsRoverPhotoDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb):Long

    @Query("SELECT * FROM mars_rover_photo_table WHERE rover_name = :roverName ORDER BY id DESC, earth_date DESC")
    fun getPhotosByRoverIDAndDate(roverName: String): PagingSource<Int, MarsRoverPhotoDb>

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName")
    fun isPhotosByDateExist(date: String, roverName:String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb)

    @Delete
    fun removeLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb)

    @Query("SELECT count(*) FROM mars_rover_photo_liked_table WHERE id = :id")
    fun isLiked(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMarsRoverPhotos(users: List<MarsRoverPhotoDb>)

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE rover_name = :roverName AND earth_date = :date")
    suspend fun dataCount(roverName: String, date: String): Int

    @Query("DELETE FROM mars_rover_photo_table")
    suspend fun clearAll()

    @Query("DELETE FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName")
    suspend fun clearByRoverIDAndDate(date: String, roverName:String)

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
