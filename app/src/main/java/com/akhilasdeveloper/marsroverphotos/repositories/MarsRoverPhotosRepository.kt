package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NO_INTERNET
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.ui.RoverRemoteMediator
import com.akhilasdeveloper.marsroverphotos.utilities.formatDateToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val remoteKeyDao: RemoteKeyDao,
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
                            launch_date_in_millis = manifest.launch_date.formatDateToMillis()!!,
                            name = src.roverName,
                            total_photos = manifest.total_photos,
                            status = manifest.status,
                            max_sol = manifest.max_sol,
                            max_date = manifest.max_date,
                            max_date_in_millis = manifest.max_date.formatDateToMillis()!!,
                            landing_date = manifest.landing_date,
                            landing_date_in_millis = manifest.landing_date.formatDateToMillis()!!,
                            description = src.roverDescription,
                            image = src.roverImage,
                            id = src.id
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

            val case =
                if (marsRoverDao.getInsertDate() == null) true else (System.currentTimeMillis() - marsRoverDao.getInsertDate()!!) > Constants.MILLIS_IN_A_DAY

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

    suspend fun updateLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb) {
        withContext(Dispatchers.IO) {
            marsRoverPhotoLikedDb.let {
                if (checkLike(it.id))
                    removeLike(marsRoverPhotoLikedDb)
                else
                    addLike(marsRoverPhotoLikedDb)
            }
        }
    }

    private suspend fun addLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb) {
        withContext(Dispatchers.IO) {
            marsRoverDao.addLike(marsRoverPhotoLikedDb = marsRoverPhotoLikedDb)
        }
    }

    private suspend fun removeLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb) {
        withContext(Dispatchers.IO) {
            marsRoverDao.removeLike(marsRoverPhotoLikedDb = marsRoverPhotoLikedDb)
        }
    }

    suspend fun isLiked(id: Long) = flow<Boolean> { emit(checkLike(id)) }

    private suspend fun checkLike(id: Long) =
        withContext(Dispatchers.IO) { marsRoverDao.isLiked(id) > 0 }

    private suspend fun refreshRoverSrcDb() {

        val response = marsRoverPhotosService.getRoverData()

        response?.roverSrc?.let { lis ->
            lis.forEach {
                insertMarsRoverSrc(
                    MarsRoverSrcDb(
                        roverDescription = it.description,
                        roverImage = it.image,
                        roverName = it.name,
                        addedDate = System.currentTimeMillis(),
                        id = it.id
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
        rover: RoverMaster
    ) = Pager(
        config = PagingConfig(pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE, enablePlaceholders = true),
        remoteMediator = RoverRemoteMediator(
            date,
            rover,
            marsRoverPhotosService,
            marsRoverDao,
            remoteKeyDao,
            marsRoverDataBase
        ),
        pagingSourceFactory = {
            marsRoverDao.getPhotosByRoverIDAndDate(roverName = rover.name, date = date)
        }
    ).flow

    suspend fun getDatePosition(roverName: String, date: String) = flow<Int> { emit(withContext(Dispatchers.IO){marsRoverDao.getDatePosition(roverName, date)}) }

    /**
     * Rover Photo END
     */

}