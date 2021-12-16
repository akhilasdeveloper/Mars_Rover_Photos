package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NO_INTERNET
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.DatePreviewData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.dao.PhotoKeyDao
import com.akhilasdeveloper.marsroverphotos.db.dao.RemoteKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverManifestTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable
import com.akhilasdeveloper.marsroverphotos.paging.MarsPagingSource
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NETWORK_TIMEOUT
import com.akhilasdeveloper.marsroverphotos.utilities.formatDateToMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val marsPhotoDao: MarsPhotoDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val marsRoverDataBase: MarsRoverDatabase,
    private val utilities: Utilities
) {

    /**
     * Rover Manifest START
     */

    private suspend fun getRoverManifest(
        data: List<MarsRoverSrcTable>,
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
    ): MarsRoverManifestTable? {

        if (utilities.isConnectedToTheInternet() && !isCache) {
            withContext(Dispatchers.IO) {
                refreshRoverManifest(roverName)
            }
        }

        return marsRoverDao.getMarsRoverManifest(roverName)
    }

    private suspend fun insertRoverManifest(marsRoverManifestTable: MarsRoverManifestTable) {
        marsRoverDao.insertMarsRoverManifestDb(marsRoverManifestTable)
    }

    private suspend fun refreshRoverManifest(roverName: String) {

        val response =
            marsRoverPhotosService.getRoverManifest(Constants.URL_MANIFEST + roverName + "/")

        response?.photo_manifest?.let { lis ->
            insertRoverManifest(
                MarsRoverManifestTable(
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
            val isEmpty = dataSrc.isEmpty()

            if (!isEmpty)
                emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, true)))

            val insertedDate = marsRoverDao.getInsertDate()
            val isExpired =
                if (insertedDate == null) true else (System.currentTimeMillis() - insertedDate) > Constants.MILLIS_IN_A_DAY

            if (isExpired || isRefresh || isEmpty) {
                if (utilities.isConnectedToTheInternet()) {
                    emit(MarsRoverSrcResponse(isLoading = true))
                    val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                        refreshRoverSrcDb()
                    }
                    if (networkJob == null) {
                        emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                    }
                } else {
                    emit(MarsRoverSrcResponse(error = ERROR_NO_INTERNET))
                }

                dataSrc = marsRoverDao.getMarsRoverSrc()
                val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                    emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, false)))
                }
                if (networkJob == null) {
                    emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                }
            }

        }.flowOn(Dispatchers.IO)
    }

    private suspend fun insertMarsRoverSrc(marsRoverSrcTable: MarsRoverSrcTable) {
        marsRoverDao.insertMarsRoverSrc(marsRoverSrcTable)
    }

    suspend fun updateLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable) {
        withContext(Dispatchers.IO) {
            marsRoverPhotoLikedTable.let {
                if (checkLike(it.id))
                    removeLike(marsRoverPhotoLikedTable)
                else
                    addLike(marsRoverPhotoLikedTable)
            }
        }
    }

    private suspend fun addLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.addLike(marsRoverPhotoLikedTable = marsRoverPhotoLikedTable)
        }
    }

    private suspend fun removeLike(marsRoverPhotoLikedTable: MarsRoverPhotoLikedTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.removeLike(marsRoverPhotoLikedTable = marsRoverPhotoLikedTable)
        }
    }

    suspend fun isLiked(id: Long) = flow<Boolean> { emit(checkLike(id)) }

    private suspend fun checkLike(id: Long) =
        withContext(Dispatchers.IO) { marsPhotoDao.isLiked(id) > 0 }

    private suspend fun refreshRoverSrcDb() {

        val response = marsRoverPhotosService.getRoverData()

        response?.roverSrc?.let { lis ->
            lis.forEach {
                insertMarsRoverSrc(
                    MarsRoverSrcTable(
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

    fun getPhotos(
        rover: RoverMaster,
        date: Long
    ): Flow<PagingData<MarsRoverPhotoTable>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
            ),
            pagingSourceFactory = {

                MarsPagingSource(
                    remoteKeyDao = remoteKeyDao,
                    roverMaster = rover,
                    marsPhotoDao = marsPhotoDao,
                    marsRoverPhotosService = marsRoverPhotosService,
                    date = date
                )
            }
        ).flow
    }

    suspend fun getDatePosition(roverName: String, date: Long) = flow<Int> {
        emit(withContext(Dispatchers.IO) {
            marsPhotoDao.getDatePosition(
                roverName,
                date
            )
        })
    }

    /**
     * Rover Photo END
     */

}