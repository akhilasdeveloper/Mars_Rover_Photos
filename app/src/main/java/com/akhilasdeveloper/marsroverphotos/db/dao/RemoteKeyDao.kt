package com.akhilasdeveloper.marsroverphotos.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.RemoteKeysTable

@Dao
interface RemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(remoteKeyTable: RemoteKeysTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeyTable: List<RemoteKeysTable>)

    @Query("SELECT * FROM remote_keys WHERE roverName = :roverName AND currDate = :currDate ")
    suspend fun remoteKeyByNameAndDate(roverName: String, currDate: Long): RemoteKeysTable

    @Query("UPDATE remote_keys SET nextDate = :nextDate WHERE roverName = :roverName AND nextDate = :currNextDate")
    suspend fun remoteKeyUpdateNextDate(nextDate: Long?, currNextDate:Long, roverName: String)

    @Query("UPDATE remote_keys SET prevDate = :prevDate WHERE roverName = :roverName AND prevDate = :currPrevDate")
    suspend fun remoteKeyUpdatePreDate(prevDate: Long?, currPrevDate:Long, roverName: String)

    @Query("DELETE FROM remote_keys")
    suspend fun deleteAll()

    @Query("DELETE FROM remote_keys WHERE roverName = :roverName AND currDate = :date")
    suspend fun deleteByRoverNameAndDate(roverName: String, date: Long)
}