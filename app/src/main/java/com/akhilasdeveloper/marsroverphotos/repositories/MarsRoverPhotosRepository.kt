package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao
) {

    private suspend fun refreshDb(api_key: String, date: String) {
        if (marsRoverDao.isPhotosByDateExist(date) <= 0) {
            val response = marsRoverPhotosService.getMarsRoverPhotos(
                api_key = api_key,
                earth_date = date
            )

            response?.photos?.forEach {
                insertMarsRoverPhoto(
                    MarsRoverPhotoDb(
                        earth_date = it.earth_date,
                        img_src = it.img_src,
                        sol = it.sol,
                        camera_full_name = it.camera.full_name,
                        camera_name = it.camera.name,
                        rover_landing_date = it.rover.landing_date,
                        rover_launch_date = it.rover.launch_date,
                        rover_name = it.rover.name,
                        rover_status = it.rover.status
                    )
                )
            }
        }
    }

    private suspend fun insertMarsRoverPhoto(marsRoverPhotoDb: MarsRoverPhotoDb) {
        marsRoverDao.insertMarsRoverPhoto(marsRoverPhotoDb)
    }

    suspend fun getPhotosByDate(date: String, api_key: String, utilities: Utilities): Flow<PagingData<MarsRoverPhotoDb>> {
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
                maxSize = 100,
                enablePlaceholders = true
            )
        ) {
            marsRoverDao.getPhotosByDate(date)
        }.flow
    }

}