package com.akhilasdeveloper.marsroverphotos.ui

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.DateGenerator
import com.akhilasdeveloper.marsroverphotos.data.DateItem
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository

class RoverDatePagingSource(private var date: String, private val dateGenerator: DateGenerator, private val marsRoverPhotosRepository: MarsRoverPhotosRepository) : PagingSource<Int, DateItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DateItem> {

        val position = params.key ?: Constants.MARS_ROVER_PHOTOS_STARTING_PAGE
        val dates =  dateGenerator.getDates(date, position, marsRoverPhotosRepository)

        return LoadResult.Page(
            data = dates,
            prevKey = if (position == Constants.MARS_ROVER_PHOTOS_STARTING_PAGE) null else position - 1,
            nextKey = if (dates.isEmpty()) null else position + 1
        )

    }

    override fun getRefreshKey(state: PagingState<Int, DateItem>): Int? {
        return null
    }
}