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
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoLikedDb
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.utilities.Event
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
    private val _dataStateRoverMaster: MutableLiveData<Event<RoverMaster>> = MutableLiveData()
    private val _dataStateDate: MutableLiveData<Long> = MutableLiveData()
    private val _dataStateLoading: MutableLiveData<Boolean> = MutableLiveData()
    private val _dataStateIsLiked: MutableLiveData<Boolean> = MutableLiveData()
    private val _dataStateDatePosition: MutableLiveData<Int> = MutableLiveData()

    private var job : Job? = null

    val dataStateDate: LiveData<Long>
        get() = _dataStateDate

    val dataStateDatePosition: LiveData<Int>
        get() = _dataStateDatePosition

    val dataStateIsLiked: LiveData<Boolean>
        get() = _dataStateIsLiked

    val dataStateLoading: LiveData<Boolean>
        get() = _dataStateLoading

    val dataStateRoverMaster: LiveData<Event<RoverMaster>>
        get() = _dataStateRoverMaster

    val dataState: LiveData<PagingData<MarsRoverPhotoDb>?>
        get() = _dataState

    val dataStateRover: LiveData<MarsRoverSrcResponse>
        get() = _dataStateRover

    val positionState: LiveData<Int>
        get() = _dataStatePosition

    fun setRoverMaster(roverMaster: RoverMaster){
        _dataStateRoverMaster.value = Event(roverMaster)
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
    fun getData(rover: RoverMaster, date: Long){
        setLoading(true)
        job?.cancel()
        job = viewModelScope.launch {
            marsRoverPhotosRepository.getPhotos(date = date, rover = rover).cachedIn(viewModelScope)
                .onEach { its->
                    _dataState.value = its
                    setLoading(false)
                }
                .launchIn(this)
        }
    }

    fun setDate(date: Long){
        _dataStateDate.value = date
    }

    fun getRoverData(isRefresh: Boolean){
        setLoading(true)
        viewModelScope.launch {
            marsRoverPhotosRepository.getRoverData(isRefresh).collect {
                setLoading(false)
                _dataStateRover.value = it
                setPosition(0)
            }
        }
    }

    fun isLiked(id: Long){
        viewModelScope.launch {
            marsRoverPhotosRepository.isLiked(id).collect {
                _dataStateIsLiked.value = it
            }
        }
    }

    fun getDatePosition(roverName: String, date: String){
        viewModelScope.launch {
            marsRoverPhotosRepository.getDatePosition(roverName, date).collect {
                _dataStateDatePosition.value = it
            }
        }
    }

    fun updateLike(marsRoverPhotoLikedDb: MarsRoverPhotoLikedDb){
        viewModelScope.launch {
            marsRoverPhotosRepository.updateLike(marsRoverPhotoLikedDb)
            isLiked(marsRoverPhotoLikedDb.id)
        }
    }

}