package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "remote_keys")
data class RemoteKeysDb(
    @PrimaryKey
    val itemID: Long,
    val roverName: String,
    val currDate: String?,
    val prevDate: String?,
    val nextDate: String?
)