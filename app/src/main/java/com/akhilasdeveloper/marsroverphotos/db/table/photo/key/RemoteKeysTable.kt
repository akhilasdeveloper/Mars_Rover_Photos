package com.akhilasdeveloper.marsroverphotos.db.table.photo.key

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "remote_keys", primaryKeys = ["roverName","currDate"])
data class RemoteKeysTable(
    val roverName: String,
    val currDate: Long,
    val prevDate: Long?,
    val nextDate: Long?
)