package com.akhilasdeveloper.marsroverphotos.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.RemoteKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.RemoteKeysTable
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.sql.SQLException

class MarsPagingSource(
    private val remoteKeyDao: RemoteKeyDao,
    private val roverMaster: RoverMaster,
    private val marsPhotoDao: MarsPhotoDao,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val date: Long

) : PagingSource<Long, MarsRoverPhotoTable>() {
    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, MarsRoverPhotoTable> {

        return try {

            withContext(Dispatchers.IO) {

                val date = params.key ?: date
                val remoteKey = remoteKeyDao.remoteKeyByNameAndDate(
                    roverName = roverMaster.name,
                    currDate = date
                )
                val nextKey: Long?
                val prevKey: Long?

                if (remoteKey == null) {
                    nextKey =
                        if (date > roverMaster.landing_date_in_millis) date.prevDate() else null
                    prevKey =
                        if (date < roverMaster.max_date_in_millis) date.nextDate() else null

                    if (date <= roverMaster.max_date_in_millis) {
                        remoteKeyDao.insertOrReplace(
                            RemoteKeysTable(
                                roverName = roverMaster.name,
                                currDate = date,
                                prevDate = prevKey,
                                nextDate = nextKey,
                            )
                        )
                    }
                } else {
                    nextKey = remoteKey.nextDate
                    prevKey = remoteKey.prevDate
                }

                var response = marsPhotoDao.getDisplayPhotosByRoverNameAndDate(
                    roverName = roverMaster.name,
                    date = date
                )
                if (response.isEmpty()) {
                    response = loadPhotos(date)

                    if (response.isEmpty() && date <= roverMaster.max_date_in_millis)
                        reConfigureRemoteKey(date, nextKey, prevKey)
                }

                Timber.d("Data Loading : $response")

                LoadResult.Page(
                    data = response,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: SQLException) {
            return LoadResult.Error(exception)
        }
    }

    private suspend fun reConfigureRemoteKey(date: Long, nextKey: Long?, prevKey: Long?) {
        remoteKeyDao.remoteKeyUpdatePreDate(
            prevDate = prevKey,
            currPrevDate = date,
            roverName = roverMaster.name
        )
        remoteKeyDao.remoteKeyUpdateNextDate(
            nextDate = nextKey,
            currNextDate = date,
            roverName = roverMaster.name
        )
    }

    private suspend fun loadPhotos(date: Long): List<MarsRoverPhotoTable> {
        val response = getMarsApi(date).toMutableList()
        val size = response.size
        if (size > 0) {
            marsPhotoDao.insertAllMarsRoverPhotos(response.apply {
                set(0, first().copy(is_placeholder = true, total_count = size))
            }.toList())
        }
        return response.toList()
    }

    private suspend fun getMarsApi(pageDate: Long): List<MarsRoverPhotoTable> {
        val url = Constants.URL_PHOTO + roverMaster.name + "/photos"
        val response = marsRoverPhotosService.getRoverPhotos(
            url = url,
            earth_date = pageDate.formatMillisToDate()
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


    override fun getRefreshKey(state: PagingState<Long, MarsRoverPhotoTable>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}