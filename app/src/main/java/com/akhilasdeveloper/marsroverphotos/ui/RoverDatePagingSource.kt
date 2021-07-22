package com.akhilasdeveloper.marsroverphotos.ui

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.Utilities

class RoverDatePagingSource(private var date: String, private val utilities: Utilities) : PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {

        val position = params.key ?: Constants.MARS_ROVER_PHOTOS_STARTING_PAGE
        val dates = mutableListOf<String>()

        utilities.formatDateToMillis(date)?.let {dateMill->
            for (i in position..(position+Constants.MARS_ROVER_PHOTOS_PAGE_SIZE)){
                dates.add(utilities.formatMillis(utilities.nextDay(dateMill,i)))
            }
        }

        return LoadResult.Page(
            data = dates,
            prevKey = if (position == Constants.MARS_ROVER_PHOTOS_STARTING_PAGE) null else position - 1,
            nextKey = if (dates.isEmpty()) null else position + 1
        )

    }

    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return null
    }
}