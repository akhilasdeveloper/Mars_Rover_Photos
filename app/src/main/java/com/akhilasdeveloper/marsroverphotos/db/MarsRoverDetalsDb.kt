package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mars_rover_details_table")
data class MarsRoverDetalsDb(
    @SerializedName("rover_landing_date") @Expose val rover_landing_date: String,
    @SerializedName("rover_launch_date") @Expose val rover_launch_date: String,
    @SerializedName("rover_name") @Expose val rover_name: String,
    @SerializedName("rover_status") @Expose val rover_status: String
){
    @PrimaryKey(autoGenerate = false)
    @SerializedName("id")
    @Expose
    var id: Int? = 5
}