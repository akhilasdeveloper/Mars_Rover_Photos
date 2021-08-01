package com.akhilasdeveloper.marsroverphotos

import androidx.paging.Pager
import com.akhilasdeveloper.marsroverphotos.data.DateItem
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DateGenerator @Inject constructor(private val utilities: Utilities) {
    /*suspend fun getDates(
        date: String,
        page: Int,
        marsRoverPhotosRepository: MarsRoverPhotosRepository
    ): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        if (page < 2) {
            utilities.formatDateToMillis(date)?.let { dateMill ->
                val start = (page - 1) * Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
                val end = start + Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
                for (i in start..end) {
                    val d = utilities.formatMillis(prevDay(dateMill, i))
                    var data: Pager<Int, MarsRoverPhotoDb>
                    withContext(Dispatchers.IO) {
                        data = marsRoverPhotosRepository.getPhotosByDate(
                            api_key = Constants.API_KEY,
                            date = d,
                            utilities = utilities
                        )
                    }

                    dates.add(
                        DateItem(
                            d, data
                        )
                    )
                }
            }
        }
        return dates
    }*/

    private fun prevDay(day: Long, mul: Int = 1) = day - (Constants.MILLIS_IN_A_DAY * mul)
}