package com.akhilasdeveloper.marsroverphotos.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDatabase
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import retrofit2.HttpException
import java.io.IOException

@ExperimentalPagingApi
class RoverRemoteMediator(
    private val date: Long,
    private val roverName: String,
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val marsRoverDataBase: MarsRoverDatabase,
    private val utilities: Utilities
) : RemoteMediator<Int, MarsRoverPhotoDb>() {

    override suspend fun initialize(): InitializeAction {
        return if (marsRoverDao.dataCount(roverName, date) > 0) {
            // Cached data is up-to-date, so there is no need to re-fetch from network.
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            // Need to refresh cached data from network; returning LAUNCH_INITIAL_REFRESH here
            // will also block RemoteMediator's APPEND and PREPEND from running until REFRESH
            // succeeds.
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarsRoverPhotoDb>
    ): MediatorResult {
        return try {
            // The network load method takes an optional after=<user.id>
            // parameter. For every page after the first, pass the last user
            // ID to let it continue from where it left off. For REFRESH,
            // pass null to load the first page.
            val loadKey = when (loadType) {
                LoadType.REFRESH -> null
                // In this example, you never need to prepend, since REFRESH
                // will always load the first page in the list. Immediately
                // return, reporting end of pagination.
                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                    /*val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(
                            endOfPaginationReached = true
                        )

                    // You must explicitly check if the last item is null when
                    // appending, since passing null to networkService is only
                    // valid for initial load. If lastItem is null it means no
                    // items were loaded after the initial REFRESH and there are
                    // no more items to load.

                    lastItem.id*/
                }
            }

            // Suspending network load via Retrofit. This doesn't need to be
            // wrapped in a withContext(Dispatcher.IO) { ... } block since
            // Retrofit's Coroutine CallAdapter dispatches on a worker
            // thread.

            val dat = utilities.formatMillis(date)
            val url = Constants.URL_PHOTO + roverName + "/photos"
            val response = marsRoverPhotosService.getRoverPhotos(
                url = url, earth_date = dat
            )

            marsRoverDataBase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    marsRoverDao.clearByRoverIDAndDate(roverName = roverName, date = date)
                }

                // Insert new users into database, which invalidates the
                // current PagingData, allowing Paging to present the updates
                // in the DB.
                response?.photos?.let { list ->
                    marsRoverDao.insertAllMarsRoverPhotos(list.map {
                        MarsRoverPhotoDb(
                            earth_date = date,
                            img_src = it.img_src,
                            sol = it.sol,
                            camera_full_name = it.camera.full_name,
                            camera_name = it.camera.name,
                            rover_id = it.rover.id,
                            rover_landing_date = it.rover.landing_date,
                            rover_launch_date = it.rover.launch_date,
                            rover_name = it.rover.name,
                            rover_status = it.rover.status
                        )
                    })
                }

            }
            MediatorResult.Success(
                endOfPaginationReached = response == null || response.photos.isEmpty()
            )


        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}