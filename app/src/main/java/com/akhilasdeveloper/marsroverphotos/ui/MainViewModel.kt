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
    private val _dataStateLoading: MutableLiveData<Boolean> = MutableLiveData()

    val dataStateDate: LiveData<Long>
        get() = _dataStateDate

    val dataStateLoading: LiveData<Boolean>
        get() = _dataStateLoading

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

    fun setEmptyPhotos(){
        _dataState.value = PagingData.empty()
    }

    fun setPosition(position: Int){
        _dataStatePosition.value = position
    }

    fun setLoading(isLoading: Boolean){
        _dataStateLoading.value = isLoading
    }

    @ExperimentalPagingApi
    fun getData(roverName: String, date: Long){
        setLoading(true)
        viewModelScope.launch {
            marsRoverPhotosRepository.getPhotos(date = date, roverName = roverName).cachedIn(viewModelScope)
                .onEach { its->
                    _dataState.value = its
                    _dataStateDate.value = date
                    setLoading(false)
                }
                .launchIn(this)
        }
    }

    fun getRoverData(isRefresh: Boolean){
        setLoading(true)
        viewModelScope.launch {
            marsRoverPhotosRepository.getRoverData(isRefresh).collect {
                setLoading(false)
                _dataStateRover.value = it
            }
        }
    }

    fun updatePhotos(marsRoverPhotoDb: MarsRoverPhotoDb){
        viewModelScope.launch {
            marsRoverPhotosRepository.updatePhotos(marsRoverPhotoDb)
        }
    }
    fun updateLike(like: Boolean, id: Int){
        viewModelScope.launch {
            marsRoverPhotosRepository.updateLike(like, id)
        }
    }

}