package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.Constants.EMPTY_NUM
import com.akhilasdeveloper.marsroverphotos.Constants.EMPTY_STR
import com.akhilasdeveloper.marsroverphotos.Constants.FALSE
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_PAGE_MAX_SIZE
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.Constants.TRUE
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.ui.RoverDatePagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val utilities: Utilities
) {

    suspend fun refreshDb(api_key: String, date: Long) {
        val dat = utilities.formatMillis(date)
        Timber.d("Date Converted : $dat")
        if (marsRoverDao.isPhotosByDateExist(date) <= 0) {
            val response = marsRoverPhotosService.getMarsRoverPhotos(
                api_key = api_key,
                earth_date = dat
            )

            response?.photos?.let {lis->
                lis.forEach {
                    insertMarsRoverPhoto(
                        MarsRoverPhotoDb(
                            earth_date = date,
                            img_src = it.img_src,
                            sol = it.sol,
                            camera_full_name = it.camera.full_name,
                            camera_name = it.camera.name,
                            rover_id = it.rover.id,
                            rover_landing_date = it.rover.landing_date,
                            rover_launch_date = it.rover.launch_date,
                            rover_name = it.rover.name,
                            rover_status = it.rover.status
                        )
                    )
                }
            }
        }
    }

    private suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb) {
        marsRoverDao.insertMarsRoverPhoto(marsRoverPhotoDb)
    }

    suspend fun getPhotosByRoverAndDate(date: Long, api_key: String, roverID: Int): Flow<PagingData<MarsRoverPhotoDb>> {
        if (utilities.isConnectedToTheInternet()) {
            withContext(Dispatchers.IO) {
                refreshDb(
                    date = date,
                    api_key = api_key
                )
            }
        }
        return Pager(
            config = PagingConfig(
                pageSize = MARS_ROVER_PHOTOS_PAGE_SIZE,
                maxSize = MARS_ROVER_PHOTOS_PAGE_MAX_SIZE,
                enablePlaceholders = true
            )
        ) {
            marsRoverDao.getPhotosByRoverIDAndDate(date = date, roverID = roverID)
        }.flow
    }

}