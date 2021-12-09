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
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.PLACEHOLDER_ID
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.PLACEHOLDER_STRING
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

@ExperimentalPagingApi
class RoverRemoteMediator(
    private val date: Long,
    private val rover: RoverMaster,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val marsRoverDataBase: MarsRoverDatabase,
    private val utilities: Utilities
) : RemoteMediator<Int, MarsRoverPhotoDb>() {

    override suspend fun initialize(): InitializeAction {
        return if (marsRoverDao.dataCount(rover.name, utilities.formatMillis(date)) > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
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
                pageKeyData as String
            }
        }

        return try {

            val url = Constants.URL_PHOTO + rover.name + "/photos"
            val isEndOfList =
                utilities.formatDateToMillis(pageDate)!! < utilities.formatDateToMillis(rover.landing_date)!! || utilities.formatDateToMillis(
                    pageDate
                )!! > utilities.formatDateToMillis(rover.max_date)!!
            var response = marsRoverPhotosService.getRoverPhotos(
                url = url, earth_date = pageDate,
                page = Constants.STARTING_PAGE_INDEX.toString()
            )

            val startPageKey = pageDate

            while (!isEndOfList) {
                var exit = false
                response?.photos?.let { list ->
                    if (list.isEmpty()) {
                        pageDate = when (loadType) {
                            LoadType.PREPEND -> {
                                utilities.datePlus(pageDate, 1)
                            }
                            LoadType.APPEND -> {
                                utilities.dateMinus(pageDate, 1)
                            }
                            LoadType.REFRESH -> {
                                rover.max_date
                            }
                        }
                    } else {
                        exit = true
                    }
                }
                if (exit)
                    break
                else
                    response = marsRoverPhotosService.getRoverPhotos(
                        url = url, earth_date = pageDate,
                        page = Constants.STARTING_PAGE_INDEX.toString()
                    )
            }

            Timber.d("marsRoverPhotosService.getRoverPhotos ## url : $url ; earth_date : $pageDate ; response : $response")



            marsRoverDataBase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    Timber.d("Refresh ***")
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
                    if (utilities.formatDateToMillis(pageDate)!! <= utilities.formatDateToMillis(
                            rover.landing_date
                        )!!
                    ) null else utilities.dateMinus(
                        pageDate,
                        1
                    )

                val prevDate =
                    if (utilities.formatDateToMillis(pageDate)!! >= utilities.formatDateToMillis(
                            rover.max_date
                        )!!
                    ) null else utilities.datePlus(
                        pageDate,
                        1
                    )

                /*marsRoverDao.insertMarsRoverPhoto(
                        MarsRoverPhotoDb(
                            earth_date = pageDate,
                            img_src = PLACEHOLDER_STRING,
                            sol = 0,
                            camera_full_name = PLACEHOLDER_STRING,
                            camera_name = PLACEHOLDER_STRING,
                            rover_id = rover.id,
                            rover_landing_date = rover.landing_date,
                            rover_launch_date = rover.launch_date,
                            rover_name = rover.name,
                            rover_status = rover.status,
                            is_placeholder = true,
                            photo_id = PLACEHOLDER_ID
                        )
                    )*/

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
                            earth_date = it.earth_date,
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
                    }.toMutableList().apply { set(0,first().copy(is_placeholder = true)) }

                    marsRoverDao.insertAllMarsRoverPhotos(data)
                    /*if (marsRoverDao.isPlaceHolderSet(
                            roverName = rover.name,
                            date = pageDate
                        ) <= 0
                    ) {
                        marsRoverDao.updatePlaceHolder(roverName = rover.name, currDate = pageDate)
                    }*/
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
                Timber.d("loadType : REFRESH")
                remoteKeys?.currDate ?: utilities.formatMillis(date)
            }
            LoadType.PREPEND -> {
                val remoteKeys = getFirstRemoteKey(state)
                remoteKeys?.prevDate ?: return MediatorResult.Success(
                    endOfPaginationReached = false
                )
                Timber.d("loadType : PREPEND")
                /*MediatorResult.Success(
                    endOfPaginationReached = false
                )*/
            }
            LoadType.APPEND -> {
                Timber.d("loadType : APPEND")
                val remoteKeys = getLastRemoteKey(state)
                val nextDate = remoteKeys?.nextDate
                nextDate ?: return MediatorResult.Success(endOfPaginationReached = false)
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