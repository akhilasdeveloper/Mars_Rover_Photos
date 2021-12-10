package com.akhilasdeveloper.marsroverphotos.data

data class RoverMaster(
    val landing_date: String,
    val landing_date_in_millis: Long,
    val launch_date: String,
    val launch_date_in_millis: Long,
    val max_date: String,
    val max_date_in_millis: Long,
    val max_sol: Int,
    val name: String,
    val status: String,
    val total_photos: Int,
    val description: String,
    val image: String,
    val id: Int
)