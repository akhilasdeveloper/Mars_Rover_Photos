package com.akhilasdeveloper.marsroverphotos.db.dao

import androidx.room.*
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverManifestTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable

@Dao
interface MarsRoverDao {

    /**
     * MarsRoverSrcDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverSrc(marsRoverSrcTable: MarsRoverSrcTable)

    @Query("SELECT * FROM mars_rover_source_table")
    fun getMarsRoverSrc(): List<MarsRoverSrcTable>

    @Query("SELECT addedDate FROM mars_rover_source_table LIMIT 1")
    suspend fun getInsertDate(): Long?

    /**
     * MarsRoverManifestDb
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarsRoverManifestDb(marsRoverManifestTable: MarsRoverManifestTable)

    @Query("SELECT * FROM mars_rover_manifest_table WHERE name = :roverName")
    fun getMarsRoverManifest(roverName: String): MarsRoverManifestTable?
}
