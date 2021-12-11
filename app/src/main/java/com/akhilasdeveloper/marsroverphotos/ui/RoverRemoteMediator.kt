package com.akhilasdeveloper.marsroverphotos.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.PLACEHOLDER_ID
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.PLACEHOLDER_STRING
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

@ExperimentalPagingApi
class RoverRemoteMediator(
    private val rover: RoverMaster,
    private val date: Long = rover.max_date_in_millis,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val marsRoverDataBase: MarsRoverDatabase
) : RemoteMediator<Int, MarsRoverPhotoDb>() {

    private val masterDate = date

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
        /*if (marsRoverDao.dataCount(rover.name, masterDate) > 0) {
                   InitializeAction.SKIP_INITIAL_REFRESH
               } else {
                   InitializeAction.LAUNCH_INITIAL_REFRESH
               }*/
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarsRoverPhotoDb>
    ): MediatorResult {

        val pageKeyData = getPageData(loadType, state)
        var pageDate = when (pageKeyData) {
            is MediatorResult.Success -> {
                return pageKeyData
            }
            else -> {
                pageKeyData as Long
            }
        }

        Timber.d(
            "**loadType : ${
                when (loadType) {
                    LoadType.REFRESH -> {
                        "REFRESH"
                    }
                    LoadType.PREPEND -> {
                        "PREPEND"
                    }
                    LoadType.APPEND -> {
                        "APPEND"
                    }
                }
            } : $pageDate"
        )

        return try {

            val url = Constants.URL_PHOTO + rover.name + "/photos"
            var isEndOfList =
                pageDate < rover.landing_date_in_millis || pageDate > rover.max_date_in_millis
            var response = marsRoverPhotosService.getRoverPhotos(
                url = url, earth_date = pageDate.formatMillisToDate()
            )

            val startPageKey = pageDate

            while (!isEndOfList) {
                var exit = false
                response?.photos?.let { list ->
                    if (list.isEmpty()) {
                        pageDate = when (loadType) {
                            LoadType.PREPEND -> {
                                pageDate.nextDate()
                            }
                            LoadType.APPEND -> {
                                pageDate.prevDate()
                            }
                            LoadType.REFRESH -> {
                                rover.max_date_in_millis
                            }
                        }
                        isEndOfList =
                            pageDate < rover.landing_date_in_millis || pageDate > rover.max_date_in_millis
                    } else {
                        exit = true
                    }
                }
                if (exit || isEndOfList)
                    break
                else
                    response = marsRoverPhotosService.getRoverPhotos(
                        url = url, earth_date = pageDate.formatMillisToDate()
                    )
            }

            marsRoverDataBase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteByRoverNameAndDate(
                        roverName = rover.name,
                        date = pageDate
                    )
                    marsRoverDao.clearByRoverIDAndDate(
                        roverName = rover.name,
                        date = pageDate
                    )
                }

                val nextDate =
                    if (pageDate <= rover.landing_date_in_millis) null else pageDate.prevDate()
                val prevDate =
                    if (pageDate >= rover.max_date_in_millis) null else pageDate.nextDate()

                response?.photos?.let { list ->

                    if (startPageKey != pageDate) {
                        when (loadType) {
                            LoadType.PREPEND -> {
                                remoteKeyDao.remoteKeyUpdatePreDate(
                                    prevDate = pageDate,
                                    currPrevDate = startPageKey,
                                    roverName = rover.name
                                )
                            }
                            LoadType.APPEND -> {
                                remoteKeyDao.remoteKeyUpdateNextDate(
                                    nextDate = pageDate,
                                    currNextDate = startPageKey,
                                    roverName = rover.name
                                )
                            }
                            LoadType.REFRESH -> {}
                        }
                    }

                    remoteKeyDao.insertOrReplace(
                        RemoteKeysDb(
                            roverName = rover.name,
                            currDate = pageDate,
                            prevDate = prevDate,
                            nextDate = nextDate,
                        )
                    )
                    val data = list.map {
                        MarsRoverPhotoDb(
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
                            id = it.id,
                            is_placeholder = false
                        )
                    }.toMutableList().apply { set(0, first().copy(is_placeholder = true)) }

                    marsRoverDao.insertAllMarsRoverPhotos(data)
                }


            }
            MediatorResult.Success(
                endOfPaginationReached = isEndOfList
            )

        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getPageData(
        loadType: LoadType,
        state: PagingState<Int, MarsRoverPhotoDb>
    ): Any {
        return when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosesToCurrentPosition(state)
                Timber.d("loadType : REFRESH ${remoteKeys?.currDate}")
                remoteKeys?.currDate ?: masterDate
            }
            LoadType.PREPEND -> {
                val remoteKeys = getFirstRemoteKey(state)
                Timber.d("loadType : PREPEND ${remoteKeys?.prevDate}")
                remoteKeys?.prevDate ?: MediatorResult.Success(
                    endOfPaginationReached = false
                )
            }
            LoadType.APPEND -> {
                val remoteKeys = getLastRemoteKey(state)
                val nextDate = remoteKeys?.nextDate
                Timber.d("loadType : APPEND $nextDate")
                nextDate ?: MediatorResult.Success(endOfPaginationReached = false)
            }
        }
    }

    private suspend fun getFirstRemoteKey(state: PagingState<Int, MarsRoverPhotoDb>): RemoteKeysDb? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { marsDb ->
                remoteKeyDao.remoteKeyByNameAndDate(
                    roverName = marsDb.rover_name,
                    currDate = marsDb.earth_date
                )
            }
    }

    private suspend fun getLastRemoteKey(state: PagingState<Int, MarsRoverPhotoDb>): RemoteKeysDb? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { marsDb ->
                remoteKeyDao.remoteKeyByNameAndDate(
                    roverName = marsDb.rover_name,
                    currDate = marsDb.earth_date
                )
            }
    }

    private suspend fun getRemoteKeyClosesToCurrentPosition(state: PagingState<Int, MarsRoverPhotoDb>): RemoteKeysDb? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { marsDb ->
                remoteKeyDao.remoteKeyByNameAndDate(
                    roverName = marsDb.rover_name,
                    currDate = marsDb.earth_date
                )
            }
        }
    }
}