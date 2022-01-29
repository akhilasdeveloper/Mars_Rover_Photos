package com.akhilasdeveloper.marsroverphotos.ui

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
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
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _dataStatePaging: MutableLiveData<Event<PagingData<MarsRoverPhotoTable>?>> =
        MutableLiveData()
    private val _dataStateLikedPhotos: MutableLiveData<PagingData<MarsRoverPhotoTable>> =
        MutableLiveData()
    private val _dataStatePosition: MutableLiveData<Int> = MutableLiveData()
    private val _dataStateRoverMaster: MutableLiveData<Event<RoverMaster>> = MutableLiveData()
    private val _dataStateDate: MutableLiveData<Long> = MutableLiveData()
    private val _dataStateLoading: MutableLiveData<Boolean> = MutableLiveData()
    private val _dataStateIsLiked: MutableLiveData<Boolean> = MutableLiveData()
    private val _dataStateInfoDialogChange: MutableLiveData<Int> = MutableLiveData()
    private var _isSavedView: Boolean = false

    private var job: Job? = null

    val isSavedView: Boolean
        get() = _isSavedView

    val dataStateDate: LiveData<Long>
        get() = _dataStateDate

    val dataStatePaging: LiveData<Event<PagingData<MarsRoverPhotoTable>?>>
        get() = _dataStatePaging

    val dataStateIsLiked: LiveData<Boolean>
        get() = _dataStateIsLiked

    val dataStateLoading: LiveData<Boolean>
        get() = _dataStateLoading

    val dataStateRoverMaster: LiveData<Event<RoverMaster>>
        get() = _dataStateRoverMaster

    val positionState: LiveData<Int>
        get() = _dataStatePosition

    init {
        savedStateHandle.get<String>("roverMaster")?.let {
            getRoverSrcDbByName(it)
        }
    }

    fun setRoverMaster(roverMaster: RoverMaster) {
        _dataStateRoverMaster.value = Event(roverMaster)
        savedStateHandle.set("roverMaster", roverMaster.name)
    }

    fun setEmptyPhotos() {
        _dataStatePaging.value = Event(PagingData.empty())
    }

    fun setPosition(position: Int) {
        _dataStatePosition.value = position
    }

    fun setLoading(isLoading: Boolean) {
        _dataStateLoading.value = isLoading
    }

    fun setIsSavedView(isSavedView: Boolean) {
        _isSavedView = isSavedView
    }

    fun setInfoDialog(state: Int) {
        _dataStateInfoDialogChange.value = state
    }

    fun getData(rover: RoverMaster, date: Long) {
        job?.cancel()
        job = viewModelScope.launch {

            marsRoverPhotosRepository.getPhotos(rover = rover, date = date)
                .cachedIn(viewModelScope)
                .onEach { its ->
                    _dataStatePaging.value = Event(its)
                }
                .launchIn(this)

        }
    }

    fun cancelPendingOperation() {
        job?.cancel()
    }

    fun getLikedPhotos(rover: RoverMaster) {
        job?.cancel()
        job = viewModelScope.launch {
            marsRoverPhotosRepository.getLikedPhotos(rover = rover)
                .cachedIn(viewModelScope)
                .onEach { its ->
//                    _dataStateLikedPhotos.value = its
                    _dataStatePaging.value = Event(its)
                }
                .launchIn(this)
        }
    }

    fun setDate(date: Long) {
        _dataStateDate.value = date
    }


    fun isLiked(id: Long) {
        viewModelScope.launch {
            marsRoverPhotosRepository.isLiked(id).collect {
                _dataStateIsLiked.value = it
            }
        }
    }

    fun updateLike(
        marsRoverPhotoTable: MarsRoverPhotoTable
    ) {
        viewModelScope.launch {
            marsRoverPhotosRepository.updateLike(marsRoverPhotoTable)
            isLiked(marsRoverPhotoTable.photo_id)
        }
    }

    fun addLike(
        marsRoverPhotoTable: MarsRoverPhotoTable
    ) {
        viewModelScope.launch {
            marsRoverPhotosRepository.addLike(marsRoverPhotoTable)
        }
    }

    private fun getRoverSrcDbByName(name: String) {
        viewModelScope.launch {
            marsRoverPhotosRepository.getRoverSrcDbByName(name).collect { rover ->
                setRoverMaster(rover)
            }
        }
    }

}