package com.akhilasdeveloper.marsroverphotos.db.dao

import androidx.constraintlayout.widget.Placeholder
import androidx.paging.PagingSource
import androidx.room.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import kotlinx.coroutines.flow.Flow


@Dao
interface MarsPhotoDao {

    /**
     * marsRoverPhotoDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverPhoto(marsRoverPhotoTable: MarsRoverPhotoTable):Long

    @Query("SELECT * FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName ORDER BY earth_date DESC,photo_id")
    fun getDisplayPhotosByRoverNameAndDate(roverName: String, date: Long): List<MarsRoverPhotoTable>

    @Query("SELECT min (foo.cnt) FROM (SELECT date, (SELECT count(*) FROM display_photos b  WHERE a.photoID >= b.photoID AND b.roverName = :roverName) AS cnt FROM display_photos a WHERE a.roverName = :roverName) foo WHERE foo.date = :date")
    fun getDatePosition(roverName: String, date: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable)

    @Delete
    fun removeLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable)

    @Query("SELECT count(*) FROM mars_rover_photo_liked_table WHERE id = :id")
    fun isLiked(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMarsRoverPhotos(users: List<MarsRoverPhotoTable>)

    @Query("SELECT count(*) FROM mars_rover_photo_table WHERE rover_name = :roverName AND earth_date = :date")
    suspend fun dataCount(roverName: String, date: Long): Int

    @Query("DELETE FROM mars_rover_photo_table")
    suspend fun clearAll()

    @Query("DELETE FROM mars_rover_photo_table WHERE earth_date = :date AND rover_name = :roverName")
    suspend fun clearByRoverIDAndDate(date: Long, roverName:String)

    @Query("SELECT * FROM mars_rover_photo_table WHERE photo_id IN (SELECT id FROM mars_rover_photo_liked_table WHERE rover_id = :roverID) ORDER BY earth_date DESC")
    fun getSavedPhotos(roverID: Int): PagingSource<Int, MarsRoverPhotoTable>

    @Query("SELECT * FROM mars_rover_photo_table WHERE photo_id IN (SELECT id FROM mars_rover_photo_liked_table) ORDER BY earth_date DESC")
    fun getSavedPhotosRaw(): List<MarsRoverPhotoTable>

}
