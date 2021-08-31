package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.Constants.ERROR_NO_INTERNET
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_PAGE_MAX_SIZE
import com.akhilasdeveloper.marsroverphotos.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.ui.RoverRemoteMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val marsRoverDataBase: MarsRoverDatabase,
    private val utilities: Utilities
) {

    /**
     * Rover Manifest START
     */

    private suspend fun getRoverManifest(
        data: List<MarsRoverSrcDb>,
        isCache: Boolean
    ): List<RoverMaster> {
        val response = mutableListOf<RoverMaster>()
        data.forEach { src ->
            withContext(Dispatchers.IO) {

                getRoverManifestData(src.roverName, isCache)?.let { manifest ->
                    response.add(
                        RoverMaster(
                            launch_date = manifest.launch_date,
                            name = src.roverName,
                            total_photos = manifest.total_photos,
                            status = manifest.status,
                            max_sol = manifest.max_sol,
                            max_date = manifest.max_date,
                            landing_date = manifest.landing_date,
                            description = src.roverDescription,
                            image = src.roverImage
                        )
                    )
                }
            }
        }
        return response
    }

    private suspend fun getRoverManifestData(
        roverName: String,
        isCache: Boolean
    ): MarsRoverManifestDb? {

        if (utilities.isConnectedToTheInternet() && !isCache) {
            withContext(Dispatchers.IO) {
                refreshRoverManifest(roverName)
            }
        }

        return marsRoverDao.getMarsRoverManifest(roverName)
    }

    private suspend fun insertRoverManifest(marsRoverManifestDb: MarsRoverManifestDb) {
        marsRoverDao.insertMarsRoverManifestDb(marsRoverManifestDb)
    }

    private suspend fun refreshRoverManifest(roverName: String) {

        val response =
            marsRoverPhotosService.getRoverManifest(Constants.URL_MANIFEST + roverName + "/")

        response?.photo_manifest?.let { lis ->
            insertRoverManifest(
                MarsRoverManifestDb(
                    landing_date = lis.landing_date,
                    launch_date = lis.launch_date,
                    max_date = lis.max_date,
                    max_sol = lis.max_sol,
                    status = lis.status,
                    total_photos = lis.total_photos,
                    name = lis.name
                )
            )
        }
    }

    /**
     * Rover Manifest END
     */

    /**
     * Rover Src START
     */

    suspend fun getRoverData(isRefresh: Boolean): Flow<MarsRoverSrcResponse> {

        return flow {
            emit(MarsRoverSrcResponse(isLoading = true))

            var dataSrc = marsRoverDao.getMarsRoverSrc()
            emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, true)))

            val case = if(marsRoverDao.getInsertDate()==null) true else (System.currentTimeMillis() - marsRoverDao.getInsertDate()!!) > Constants.MILLIS_IN_A_DAY

            if (case || isRefresh) {
                if (utilities.isConnectedToTheInternet()) {
                    emit(MarsRoverSrcResponse(isLoading = true))
                    withContext(Dispatchers.IO) {
                        refreshRoverSrcDb()
                    }
                } else {
                    emit(MarsRoverSrcResponse(error = ERROR_NO_INTERNET))
                }

                dataSrc = marsRoverDao.getMarsRoverSrc()
                emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, false)))
            }

        }.flowOn(Dispatchers.IO)
    }

    private suspend fun insertMarsRoverSrc(marsRoverSrcDb: MarsRoverSrcDb) {
        marsRoverDao.insertMarsRoverSrc(marsRoverSrcDb)
    }

    private suspend fun refreshRoverSrcDb() {

        val response = marsRoverPhotosService.getRoverData()

        response?.roverSrc?.let { lis ->
            lis.forEach {
                insertMarsRoverSrc(
                    MarsRoverSrcDb(
                        roverDescription = it.description,
                        roverImage = it.image,
                        roverName = it.name,
                        addedDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Rover Src END
     */

    /**
     * Rover Photo START
     */

    @ExperimentalPagingApi
    fun getPhotos(
        date: Long,
        roverName: String
    ) = Pager(
        config = PagingConfig(pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE),
        remoteMediator = RoverRemoteMediator(
            date,
            roverName,
            marsRoverPhotosService,
            marsRoverDao,
            marsRoverDataBase,
            utilities
        )
    ) {
        marsRoverDao.getPhotosByRoverIDAndDate(roverName = roverName, date = date)
    }.flow

    suspend fun getPhotosByRoverAndDate(
        date: Long,
        roverName: String
    ): Flow<PagingData<MarsRoverPhotoDb>> {
        if (utilities.isConnectedToTheInternet()) {
            withContext(Dispatchers.IO) {
                refreshDb(
                    date = date,
                    roverName = roverName
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
            marsRoverDao.getPhotosByRoverIDAndDate(date = date, roverName = roverName)
        }.flow
    }

    private suspend fun refreshDb(date: Long, roverName: String) {

        val dat = utilities.formatMillis(date)
        val url = Constants.URL_PHOTO + roverName + "/photos"

        if (marsRoverDao.isPhotosByDateExist(date, roverName) <= 0) {

            val response = marsRoverPhotosService.getRoverPhotos(
                earth_date = dat,
                url = url
            )

            response?.photos?.let { lis ->
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

    /**
     * Rover Photo END
     */

}