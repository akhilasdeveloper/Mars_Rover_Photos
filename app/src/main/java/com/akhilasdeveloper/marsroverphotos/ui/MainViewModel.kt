package com.akhilasdeveloper.marsroverphotos.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository
) : ViewModel() {

    private val _dataState: MutableLiveData<PagingData<MarsRoverPhotoDb>?> = MutableLiveData()
    private val _dataStatePosition: MutableLiveData<Int> = MutableLiveData()
    private val _dataStateRover: MutableLiveData<MarsRoverSrcResponse> = MutableLiveData()
    private val _dataStateRoverMaster: MutableLiveData<RoverMaster> = MutableLiveData()
    private val _dataStateDate: MutableLiveData<Long> = MutableLiveData()

    val dataStateDate: LiveData<Long>
        get() = _dataStateDate

    val dataStateRoverMaster: LiveData<RoverMaster>
        get() = _dataStateRoverMaster

    val dataState: LiveData<PagingData<MarsRoverPhotoDb>?>
        get() = _dataState

    val dataStateRover: LiveData<MarsRoverSrcResponse>
        get() = _dataStateRover

    val positionState: LiveData<Int>
        get() = _dataStatePosition

    fun setRoverMaster(roverMaster: RoverMaster){
        _dataStateRoverMaster.value = roverMaster
    }

    fun setPosition(position: Int){
        _dataStatePosition.value = position
    }

    @ExperimentalPagingApi
    fun getData(roverName: String, date: Long){
        viewModelScope.launch {
            marsRoverPhotosRepository.getPhotos(date = date, roverName = roverName).cachedIn(viewModelScope)
                .onEach { its->
                    _dataState.value = its
                    _dataStateDate.value = date
                }
                .launchIn(this)
        }
    }

    fun getRoverData(isRefresh: Boolean){
        viewModelScope.launch {
            marsRoverPhotosRepository.getRoverData(isRefresh).collect {
                _dataStateRover.value = it
            }
        }
    }

}