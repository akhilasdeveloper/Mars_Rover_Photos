package com.akhilasdeveloper.marsroverphotos.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.marsroverphotos.Constants.NETWORK_TIMEOUT
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosApiResponse
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val utilities: Utilities
) : ViewModel() {

    private val _dataState: MutableLiveData<MarsRoverPhotosApiResponse> = MutableLiveData()

    val dataState: LiveData<MarsRoverPhotosApiResponse>
        get() = _dataState

    fun getData(
        sol : String,
        api_key : String
    ) {

        if (!utilities.isConnectedToTheInternet()) {
            val mainResponse: MarsRoverPhotosApiResponse = MarsRoverPhotosApiResponse(
                arrayListOf()
            )
            _dataState.value = mainResponse
            return
        }


        viewModelScope.launch {
            val job = withTimeoutOrNull(NETWORK_TIMEOUT) {
                marsRoverPhotosRepository.getMarsRoverPhotos(
                    sol = sol,
                    api_key = api_key
                )
                    .onEach { dataState ->
                        _dataState.value = dataState
                    }
                    .launchIn(this)
            }

            if (job == null) {
                val mainResponse: MarsRoverPhotosApiResponse = MarsRoverPhotosApiResponse(
                    arrayListOf()
                )
                _dataState.value = mainResponse
            }
        }
    }
}