package com.akhilasdeveloper.marsroverphotos.repositories

import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosApiResponse
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(private val marsRoverPhotosService: MarsRoverPhotosService) {

    suspend fun getMarsRoverPhotos(
        sol : String,
        api_key : String
    ): Flow<MarsRoverPhotosApiResponse> = flow {
        try{
            val data = marsRoverPhotosService.getMarsRoverPhotos(
                sol = sol,
                api_key = api_key
            )
            data?.let {
                emit(data)
            }?: kotlin.run {
                emit(
                    MarsRoverPhotosApiResponse(
                        arrayListOf()
                    )
                )
            }
            Timber.d("Data: $data")
        }catch (e: Exception){
            Timber.d("Data Error: ${e.toString()}")
            emit(
                MarsRoverPhotosApiResponse(
                    arrayListOf()
                )
            )
        }
    }

}