package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NO_INTERNET
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.data.RoverData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.dao.RemoteKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.RemoteKeysTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable
import com.akhilasdeveloper.marsroverphotos.paging.MarsPagingSource
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NETWORK_TIMEOUT
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ROVER_STATUS_ACTIVE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val marsPhotoDao: MarsPhotoDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val utilities: Utilities
) {

    /**
     * Rover Manifest START
     */

    private suspend fun calculateMaxDates(response: RoverData?) {
        withContext(Dispatchers.IO) {
            response?.roverSrc?.forEach { rover ->
                var currentMaxDate = ""
                var startingMaxDate = System.currentTimeMillis().formatMillisToDate()

                if (rover.status == ROVER_STATUS_ACTIVE) {
                    currentMaxDate = rover.max_date
                    Timber.d("calculateMaxDates5 startingMaxDate : $startingMaxDate")
                    Timber.d("calculateMaxDates5 currentMaxDate : $currentMaxDate")
                    var list = listOf<MarsRoverPhotoTable>()
                    while (currentMaxDate != startingMaxDate && list.isEmpty()) {

                        val date = startingMaxDate.formatDateToMillis()!!
                        val remoteKey = remoteKeyDao.remoteKeyByNameAndDate(
                            roverName = rover.name,
                            currDate = date
                        )
                        val nextKey: Long?
                        val prevKey: Long?
                        Timber.d("calculateMaxDates5 currentMaxDate : $currentMaxDate")
                        Timber.d("calculateMaxDates5 list : $list")

                        if (remoteKey == null) {
                            nextKey =
                                if (date > rover.landing_date.formatDateToMillis()!!) date.prevDate() else null
                            prevKey =
                                if (date < System.currentTimeMillis().formatMillisToDate()
                                        .formatDateToMillis()!!
                                ) date.nextDate() else null
                            remoteKeyDao.insertOrReplace(
                                RemoteKeysTable(
                                    roverName = rover.name,
                                    currDate = date,
                                    prevDate = prevKey,
                                    nextDate = nextKey,
                                )
                            )
                        } else {
                            nextKey = remoteKey.nextDate
                            prevKey = remoteKey.prevDate
                        }

                        Timber.d("calculateMaxDates5 nextKey : $nextKey")
                        Timber.d("calculateMaxDates5 prevKey : $prevKey")

                        list = marsPhotoDao.getDisplayPhotosByRoverNameAndDate(
                            roverName = rover.name,
                            date = date
                        )
                        Timber.d("calculateMaxDates5 list1 : $list")
                        if (list.isEmpty()) {
                            list = loadPhotos(date.formatMillisToDate(), rover.name)
                            Timber.d("calculateMaxDates5 list2 : $list")
                            if (list.isEmpty()) {
                                reConfigureRemoteKey(date, nextKey, prevKey, rover.name)
                            }
                        }

                        startingMaxDate = nextKey?.formatMillisToDate()!!
                    }

                    marsRoverDao.updateMaxDate(startingMaxDate, rover.name)
                    Timber.d("calculateMaxDates5 ${rover.name} : $startingMaxDate")
                }

            }
        }
    }

    private suspend fun reConfigureRemoteKey(
        date: Long,
        nextKey: Long?,
        prevKey: Long?,
        name: String
    ) {
        remoteKeyDao.remoteKeyUpdatePreDate(
            prevDate = prevKey,
            currPrevDate = date,
            roverName = name
        )
        remoteKeyDao.remoteKeyUpdateNextDate(
            nextDate = nextKey,
            currNextDate = date,
            roverName = name
        )
    }

    private suspend fun loadPhotos(date: String, name: String): List<MarsRoverPhotoTable> {
        val response = getMarsApi(date, name).toMutableList()
        val size = response.size
        if (size > 0) {
            marsPhotoDao.insertAllMarsRoverPhotos(response.apply {
                set(0, first().copy(is_placeholder = true, total_count = size))
            }.toList())
        }
        return response.toList()
    }

    private suspend fun getMarsApi(pageDate: String, name: String): List<MarsRoverPhotoTable> {
        val url = Constants.URL_PHOTO + name + "/photos"
        val response = marsRoverPhotosService.getRoverPhotos(
            url = url,
            earth_date = pageDate
        )
        return mapToMarsRoverPhotoDb(response?.photos)
    }

    private fun mapToMarsRoverPhotoDb(photos: List<Photo>?): List<MarsRoverPhotoTable> {
        return photos?.map {
            MarsRoverPhotoTable(
                earth_date = it.earth_date.formatDateToMillis()!!,
                img_src = it.img_src,
                sol = it.sol,
                camera_full_name = it.camera.full_name,
                camera_name = it.camera.name,
                rover_id = it.rover.id,
                rover_landing_date = it.rover.landing_date,
                rover_launch_date = it.rover.launch_date,
                rover_name = it.rover.name,
                rover_status = it.rover.status,
                photo_id = it.id,
                is_placeholder = false
            )
        } ?: listOf()
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

            var dataSrc = marsRoverDao.getMarsRoverSrc().map { src ->
                RoverMaster(
                    launch_date = src.launch_date,
                    launch_date_in_millis = src.launch_date.formatDateToMillis()!!,
                    name = src.roverName,
                    total_photos = src.total_photos,
                    status = src.status,
                    max_sol = src.max_sol,
                    max_date = src.max_date,
                    max_date_in_millis = src.max_date.formatDateToMillis()!!,
                    landing_date = src.landing_date,
                    landing_date_in_millis = src.landing_date.formatDateToMillis()!!,
                    description = src.roverDescription,
                    image = src.roverImage,
                    id = src.id
                )
            }
            val isEmpty = dataSrc.isEmpty()

            if (!isEmpty)
                emit(MarsRoverSrcResponse(data = dataSrc))

            val insertedDate = marsRoverDao.getInsertDate()
            val isExpired =
                if (insertedDate == null) true else (System.currentTimeMillis() - insertedDate) > Constants.MILLIS_IN_A_DAY

            if ((isExpired || isRefresh) && !isEmpty)
                emit(MarsRoverSrcResponse(message = "Syncing Database"))

            if (isExpired || isRefresh || isEmpty) {
                if (utilities.isConnectedToTheInternet()) {
                    emit(MarsRoverSrcResponse(isLoading = true))
                    val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                        try {
                            refreshRoverSrcDb()
                        } catch (exception: Exception) {
                            emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                            Timber.e("refreshRoverSrcDb() : $exception")
                            return@withTimeoutOrNull
                        }
                    }
                    if (networkJob == null) {
                        emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                        return@flow
                    }
                } else {
                    emit(MarsRoverSrcResponse(error = ERROR_NO_INTERNET))
                    return@flow
                }

                dataSrc = marsRoverDao.getMarsRoverSrc().map { src ->
                    RoverMaster(
                        launch_date = src.launch_date,
                        launch_date_in_millis = src.launch_date.formatDateToMillis()!!,
                        name = src.roverName,
                        total_photos = src.total_photos,
                        status = src.status,
                        max_sol = src.max_sol,
                        max_date = src.max_date,
                        max_date_in_millis = src.max_date.formatDateToMillis()!!,
                        landing_date = src.landing_date,
                        landing_date_in_millis = src.landing_date.formatDateToMillis()!!,
                        description = src.roverDescription,
                        image = src.roverImage,
                        id = src.id
                    )
                }
                val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                    try {
                        emit(MarsRoverSrcResponse(data = dataSrc))
                    } catch (exception: Exception) {
                        emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                        Timber.e("getRoverManifest : $exception")
                        return@withTimeoutOrNull
                    }
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

    suspend fun updateLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsRoverPhotoTable.let {
                if (checkLike(it.photo_id))
                    removeLike(marsRoverPhotoTable)
                else
                    addLike(marsRoverPhotoTable)
            }
        }
    }

    private suspend fun addLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.addLike(
                marsRoverPhotoLikedTable = MarsRoverPhotoLikedTable(
                    id = marsRoverPhotoTable.photo_id,
                    rover_id = marsRoverPhotoTable.rover_id
                )
            )
        }
    }

    private suspend fun removeLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.removeLike(
                marsRoverPhotoLikedTable = MarsRoverPhotoLikedTable(
                    id = marsRoverPhotoTable.photo_id,
                    rover_id = marsRoverPhotoTable.rover_id
                )
            )
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
                        id = it.id,
                        launch_date = it.launch_date,
                        landing_date = it.landing_date,
                        max_date = it.max_date,
                        max_sol = it.max_sol,
                        status = it.status,
                        total_photos = it.total_photos
                    )
                )
            }
        }

        calculateMaxDates(response)
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

    fun getLikedPhotos(
        rover: RoverMaster
    ): Flow<PagingData<MarsRoverPhotoTable>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
            ),
            pagingSourceFactory = {
                marsPhotoDao.getSavedPhotos(roverID = rover.id)
            }
        ).flow
    }

    /**
     * Rover Photo END
     */

}