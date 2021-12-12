package com.akhilasdeveloper.marsroverphotos.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.DatePreviewData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.dao.PhotoKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.key.PhotoDatesTable
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.STARTING_PAGE_INDEX
import com.akhilasdeveloper.marsroverphotos.utilities.prevDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.sql.SQLException

class MarsPagingSource(
    private val photoKeyDao: PhotoKeyDao,
    private val roverMaster: RoverMaster,
    private val marsPhotoDao: MarsPhotoDao,
    private val date: Long
) : PagingSource<Int, DatePreviewData>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DatePreviewData> {

        return try {

            withContext(Dispatchers.IO){

                var page = params.key ?: STARTING_PAGE_INDEX
                val size = MARS_ROVER_PHOTOS_PAGE_SIZE
                Timber.d("DatePreviewData : page1 : $page")
                if (params.key==null){
                    val count = photoKeyDao.getAllPhotoDatesCountByDate(roverName = roverMaster.name, date = date)
                    page = (count / size).plus(1)
                    Timber.d("DatePreviewData : page2 : $page")
                }

                configureDateKey()


                val dates = photoKeyDao.getPhotoDatesByPage(roverName = roverMaster.name, pageSize = size, pageOffset = (page-1) * size)
                val response = dates.map {
                    DatePreviewData(
                        roverName = roverMaster.name,
                        currentDate = it.date,
                        photos = marsPhotoDao.getDisplayPhotosByRoverNameAndDate(roverName = it.roverName, date = it.date)
                    )
                }
                LoadResult.Page(
                    data = response,
                    prevKey = if (page <= STARTING_PAGE_INDEX) null else page - 1,
                    nextKey = if (response.isEmpty()) null else page + 1
                )
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: SQLException) {
            return LoadResult.Error(exception)
        }
    }

    private suspend fun configureDateKey() {
        var currDate = roverMaster.max_date_in_millis
        val endDate = roverMaster.landing_date_in_millis

        if (checkIfDatesKeysEmpty()){
            while (currDate>=endDate) {
                photoKeyDao.insertPhotoDate(
                    PhotoDatesTable(
                        roverName = roverMaster.name,
                        date = currDate
                    )
                )
                currDate = currDate.prevDate()
            }
        }else{
            while (!checkIfDatesKeysExist(currDate)){
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

    private fun checkIfDatesKeysExist(date: Long) = photoKeyDao.getPhotoDatesCountByDate(roverName = roverMaster.name, date = date) > 0

    private fun checkIfDatesKeysEmpty() = photoKeyDao.getPhotoDatesCount(roverName = roverMaster.name) <= 0

    override fun getRefreshKey(state: PagingState<Int, DatePreviewData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}