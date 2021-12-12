package com.akhilasdeveloper.marsroverphotos.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.data.DatePreviewData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.PhotoKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.DisplayPhotosTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.PhotoDatesTable
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MARS_ROVER_PHOTOS_DISPLAY_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.STARTING_PAGE_INDEX
import com.akhilasdeveloper.marsroverphotos.utilities.formatDateToMillis
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDate
import com.akhilasdeveloper.marsroverphotos.utilities.prevDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.sql.SQLException

class MarsPagingSource(
    private val photoKeyDao: PhotoKeyDao,
    private val roverMaster: RoverMaster,
    private val marsPhotoDao: MarsPhotoDao,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val count: Int
) : PagingSource<Int, DatePreviewData>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DatePreviewData> {

        return try {

            withContext(Dispatchers.IO) {

                var page = params.key ?: STARTING_PAGE_INDEX
                var size = MARS_ROVER_PHOTOS_PAGE_SIZE
                val offset = count % size

                if (params.key == null) {
                    page = (count / size).plus(1)
                }

                var pageOffset = ((page - 1) * size).plus(offset)

                if (page == STARTING_PAGE_INDEX - 1 && offset != 0) {
                    size = offset
                    pageOffset = 0
                }

                configureDateKey()

                val dates = photoKeyDao.getPhotoDatesByPage(
                    roverName = roverMaster.name,
                    pageSize = size,
                    pageOffset = pageOffset
                )

                loadPhotos(dates)

                val response = dates.map {
                    DatePreviewData(
                        roverName = roverMaster.name,
                        currentDate = it.date,
                        photos = marsPhotoDao.getDisplayPhotosByRoverNameAndDate(
                            roverName = it.roverName,
                            date = it.date
                        )
                    )
                }
                LoadResult.Page(
                    data = response,
                    prevKey = if (page <= STARTING_PAGE_INDEX) if (page != STARTING_PAGE_INDEX - 1 && offset != 0) STARTING_PAGE_INDEX - 1 else null else page - 1,
                    nextKey = if (response.isEmpty()) null else page + 1
                )
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: SQLException) {
            return LoadResult.Error(exception)
        }
    }

    private fun loadPhotos(dates: List<PhotoDatesTable>) {
        CoroutineScope(Dispatchers.IO).launch {

            dates.forEach { date ->
                val count = marsPhotoDao.getDisplayPhotosCountByRoverNameAndDate(
                    roverName = date.roverName,
                    date = date.date
                )
                if (count <= 0) {
                    val response = getMarsApi(date.date)
                    val size = response.size
                    marsPhotoDao.insertAllMarsRoverPhotos(response)
                    photoKeyDao.insertAllDisplayPhotos(
                        response.map {
                            DisplayPhotosTable(
                                roverName = it.rover_name,
                                date = it.earth_date,
                                photoID = it.photo_id
                            )
                        }.subList(
                            0,
                            if (size < MARS_ROVER_PHOTOS_DISPLAY_PAGE_SIZE) size else MARS_ROVER_PHOTOS_DISPLAY_PAGE_SIZE
                        )
                    )
                }
            }
        }
    }

    private suspend fun getMarsApi(pageDate: Long): List<MarsRoverPhotoTable> {
        val url = Constants.URL_PHOTO + roverMaster.name + "/photos"
        val response = marsRoverPhotosService.getRoverPhotosByPage(
            url = url,
            earth_date = pageDate.formatMillisToDate(),
            page_no = STARTING_PAGE_INDEX
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

    private suspend fun configureDateKey() {
        var currDate = roverMaster.max_date_in_millis
        val endDate = roverMaster.landing_date_in_millis

        if (checkIfDatesKeysEmpty()) {
            while (currDate >= endDate) {
                photoKeyDao.insertPhotoDate(
                    PhotoDatesTable(
                        roverName = roverMaster.name,
                        date = currDate
                    )
                )
                currDate = currDate.prevDate()
            }
        } else {
            while (!checkIfDatesKeysExist(currDate)) {
                photoKeyDao.insertPhotoDate(
                    PhotoDatesTable(
                        roverName = roverMaster.name,
                        date = currDate
                    )
                )
                currDate = currDate.prevDate()
            }
        }
    }

    private fun checkIfDatesKeysExist(date: Long) =
        photoKeyDao.getPhotoDatesCountByDate(roverName = roverMaster.name, date = date) > 0

    private fun checkIfDatesKeysEmpty() =
        photoKeyDao.getPhotoDatesCount(roverName = roverMaster.name) <= 0

    override fun getRefreshKey(state: PagingState<Int, DatePreviewData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}