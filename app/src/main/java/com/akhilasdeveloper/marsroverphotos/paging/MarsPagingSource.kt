package com.akhilasdeveloper.marsroverphotos.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.data.DatePreviewData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import retrofit2.HttpException
import java.io.IOException
import java.sql.SQLException
/*

class MarsPagingSource(private val marsRoverDao: MarsRoverDao,private val roverMaster: RoverMaster, private val marsRoverPhotosRepository: MarsRoverPhotosRepository) : PagingSource<Int, DatePreviewData>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DatePreviewData> {
        val page = params.key ?: roverMaster.max_date_in_millis
        return try {
            val response = service.getCatImages(page = page, size = params.loadSize)
            LoadResult.Page(
                data = response,
                prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (response.isEmpty()) null else page + 1
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: SQLException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DatePreviewData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}*/
