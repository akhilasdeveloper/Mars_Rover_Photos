package com.akhilasdeveloper.marsroverphotos.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

@ExperimentalPagingApi
class RoverRemoteMediator(
    private val rover: RoverMaster,
    private val date: Long = rover.max_date_in_millis,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDataBase: MarsRoverDatabase
) : RemoteMediator<Int, MarsRoverPhotoDb>() {

    private val masterDate = date

    override suspend fun initialize(): InitializeAction {
        return if (marsRoverDataBase.getMarsRoverDao().dataCount(rover.name, masterDate) > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarsRoverPhotoDb>
    ): MediatorResult {

        var pageDate = when (val pageKeyData = getPageData(loadType, state)) {
            is MediatorResult.Success -> {
                return pageKeyData
            }
            else -> {
                pageKeyData as Long
            }
        }

        val startPageKey = pageDate

        printLog(loadType, pageDate)

        return try {

            var response = getMarsApi(pageDate).toMutableList()

            var nextDate = findNextDate(pageDate)
            var prevDate = findPrevDate(pageDate)

            reConfigureRemoteKey(startPageKey, pageDate, loadType)
            addRemoteKey(pageDate,prevDate,nextDate)

            val data = response.apply {
                if (isNotEmpty()) {
                    set(0, first().copy(is_placeholder = true))
                }
            }.toMutableList()

            while (!isEndOfList(pageDate) && data.size < MARS_ROVER_PHOTOS_PAGE_SIZE) {

                pageDate = calculateNextPageDate(loadType, pageDate)

                response = getMarsApi(pageDate).toMutableList()

                nextDate = findNextDate(pageDate)
                prevDate = findPrevDate(pageDate)

                reConfigureRemoteKey(startPageKey, pageDate, loadType)
                addRemoteKey(pageDate,prevDate,nextDate)

                data.addAll(response.apply {
                    if (isNotEmpty()) {
                        set(0, first().copy(is_placeholder = true))
                    }
                })
            }

            marsRoverDataBase.withTransaction {
                /*if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteByRoverNameAndDate(
                        roverName = rover.name,
                        date = pageDate
                    )
                    marsRoverDao.clearByRoverIDAndDate(
                        roverName = rover.name,
                        date = pageDate
                    )
                }*/
                marsRoverDataBase.getMarsRoverDao().insertAllMarsRoverPhotos(data)
            }



            MediatorResult.Success(
                endOfPaginationReached = isEndOfList(pageDate)
            )

        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private fun printLog(loadType: LoadType, pageDate: Long) {
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
    }

    private suspend fun addRemoteKey(currDate: Long, prevDate: Long?, nextDate: Long?) {
        marsRoverDataBase.withTransaction {
            marsRoverDataBase.getRemoteKeysDao().insertOrReplace(
                RemoteKeysDb(
                    roverName = rover.name,
                    currDate = currDate,
                    prevDate = prevDate,
                    nextDate = nextDate,
                )
            )
        }
    }

    private suspend fun reConfigureRemoteKey(
        startPageKey: Long,
        pageDate: Long,
        loadType: LoadType
    ) {
        marsRoverDataBase.withTransaction {
            if (startPageKey != pageDate) {
                when (loadType) {
                    LoadType.PREPEND -> {
                        marsRoverDataBase.getRemoteKeysDao().remoteKeyUpdatePreDate(
                            prevDate = pageDate,
                            currPrevDate = startPageKey,
                            roverName = rover.name
                        )
                    }
                    LoadType.APPEND -> {
                        marsRoverDataBase.getRemoteKeysDao().remoteKeyUpdateNextDate(
                            nextDate = pageDate,
                            currNextDate = startPageKey,
                            roverName = rover.name
                        )
                    }
                    LoadType.REFRESH -> {}
                }
            }
        }
    }

    private fun findPrevDate(pageDate: Long): Long? =
        if (pageDate >= rover.max_date_in_millis) null else pageDate.nextDate()

    private fun findNextDate(pageDate: Long): Long? =
        if (pageDate <= rover.landing_date_in_millis) null else pageDate.prevDate()

    private fun calculateNextPageDate(loadType: LoadType, pageDate: Long): Long =
        when (loadType) {
            LoadType.PREPEND -> {
                pageDate.nextDate()
            }
            LoadType.APPEND -> {
                pageDate.prevDate()
            }
            LoadType.REFRESH -> {
                pageDate.prevDate()
            }
        }


    private suspend fun getMarsApi(pageDate: Long): List<MarsRoverPhotoDb> {
        val url = Constants.URL_PHOTO + rover.name + "/photos"
        val response = marsRoverPhotosService.getRoverPhotos(
            url = url,
            earth_date = pageDate.formatMillisToDate()
        )
        return mapToMarsRoverPhotoDb(response?.photos)
    }

    private fun mapToMarsRoverPhotoDb(photos: List<Photo>?): List<MarsRoverPhotoDb> {
        return photos?.map {
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
                photo_id = it.id,
                is_placeholder = false
            )
        } ?: listOf()
    }

    private fun isEndOfList(pageDate: Long) =
        pageDate < rover.landing_date_in_millis || pageDate > rover.max_date_in_millis

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
                marsRoverDataBase.getRemoteKeysDao().remoteKeyByNameAndDate(
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
                marsRoverDataBase.getRemoteKeysDao().remoteKeyByNameAndDate(
                    roverName = marsDb.rover_name,
                    currDate = marsDb.earth_date
                )
            }
    }

    private suspend fun getRemoteKeyClosesToCurrentPosition(state: PagingState<Int, MarsRoverPhotoDb>): RemoteKeysDb? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { marsDb ->
                marsRoverDataBase.getRemoteKeysDao().remoteKeyByNameAndDate(
                    roverName = marsDb.rover_name,
                    currDate = marsDb.earth_date
                )
            }
        }
    }
}