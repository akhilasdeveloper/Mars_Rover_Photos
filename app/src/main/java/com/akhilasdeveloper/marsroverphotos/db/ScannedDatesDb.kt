package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "scanned_dates_table")
data class ScannedDatesDb(
    @SerializedName("date") @Expose val date: Long,
){
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    @Expose
    var id: Int? = null
}