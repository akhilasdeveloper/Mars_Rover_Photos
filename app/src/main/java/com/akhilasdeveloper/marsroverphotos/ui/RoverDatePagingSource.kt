package com.akhilasdeveloper.marsroverphotos.ui

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_STARTING_PAGE
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDetalsDb
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.db.ScannedDatesDb
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.sql.SQLException
import java.util.*

/*
class RoverDatePagingSource(
    private var rover: MarsRoverDetalsDb,
    private val marsRoverDao: MarsRoverDao,
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val utilities: Utilities
) : PagingSource<Int, MarsRoverPhotoDb>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarsRoverPhotoDb> {

        val position = params.key ?: MARS_ROVER_PHOTOS_STARTING_PAGE

        return try {

            var response = listOf<MarsRoverPhotoDb>()

            var latestDate = utilities.formatDateToMillis(utilities.formatMillis(System.currentTimeMillis()))!!
            withContext(Dispatchers.IO) {
                marsRoverDao.latestScannedDate()?.let {
                    latestDate = prevDay(it)
                }
            }

            while (response.isEmpty() && prevDay(utilities.formatDateToMillis(rover.rover_landing_date)!!) != latestDate) {

                withContext(Dispatchers.IO) {
                    marsRoverDao.insertScannedDate(ScannedDatesDb(latestDate))
                    if (utilities.isConnectedToTheInternet()) {
                        marsRoverPhotosRepository.refreshDb(
                            date = latestDate)
                    }

                    response = marsRoverDao.getPhotosByRoverID(
                        roverID = rover.id!!,
                        page = position,
                        size = params.loadSize
                    )
                }

                latestDate = prevDay(latestDate)
            }

            LoadResult.Page(
                data = response,
                prevKey = if (position == MARS_ROVER_PHOTOS_STARTING_PAGE) null else position - 1,
                nextKey = if (response.isEmpty()) null else position + 1
            )

        } catch (exception: IOException) {
            Timber.e("IOExceptionQ ${exception.stackTraceToString()}")
            LoadResult.Error(exception)
        } catch (exception: SQLException) {
            Timber.e("SQLExceptionQ ${exception.stackTraceToString()}")
            LoadResult.Error(exception)
        } catch (exception: Exception) {
            Timber.e("ExceptionQ ${exception.stackTraceToString()}")
            LoadResult.Error(exception)
        }

    }

    override fun getRefreshKey(state: PagingState<Int, MarsRoverPhotoDb>): Int? {
        return null
    }

    private fun nextDay(day: Long, mul: Int = 1) = day + (Constants.MILLIS_IN_A_DAY * mul)
    private fun prevDay(day: Long, mul: Int = 1) = day - (Constants.MILLIS_IN_A_DAY * mul)
}*/
