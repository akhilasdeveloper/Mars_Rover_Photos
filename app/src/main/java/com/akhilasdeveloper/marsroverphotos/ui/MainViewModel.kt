package com.akhilasdeveloper.marsroverphotos.ui

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.utilities.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _dataStatePaging: MutableLiveData<Event<PagingData<MarsRoverPhotoTable>?>?> =
        MutableLiveData()
    private val _dataStatePosition: MutableLiveData<Event<Int>> = MutableLiveData()
    private val _dataStateRoverMaster: MutableLiveData<Event<RoverMaster>> = MutableLiveData()
    private val _dataStateInfoDialogChange: MutableLiveData<Int> = MutableLiveData()
    private var _isSavedView: Boolean = false

    private var job: Job? = null

    val isSavedView: Boolean
        get() = _isSavedView


    val dataStatePaging: LiveData<Event<PagingData<MarsRoverPhotoTable>?>?>
        get() = _dataStatePaging

    val dataStateRoverMaster: LiveData<Event<RoverMaster>>
        get() = _dataStateRoverMaster

    val dataStateInfoDialogChange: LiveData<Int>
        get() = _dataStateInfoDialogChange

    val positionState: LiveData<Event<Int>>
        get() = _dataStatePosition

    init {
        savedStateHandle.get<String>("roverMaster")?.let {
            getRoverSrcDbByName(it)
        }
        savedStateHandle.get<Boolean>("isSavedView")?.let {
            setIsSavedView(it)
        }
    }

    fun setRoverMaster(roverMaster: RoverMaster) {
        _dataStateRoverMaster.value = Event(roverMaster)
        savedStateHandle.set("roverMaster", roverMaster.name)
    }

    fun setEmptyPhotos() {
        _dataStatePaging.value = null
    }

    fun setPosition(position: Int) {
        _dataStatePosition.value = Event(position)
    }

    fun setIsSavedView(isSavedView: Boolean) {
        _isSavedView = isSavedView
        savedStateHandle.set("isSavedView", isSavedView)
    }

    fun setInfoDialog(state: Int) {
        _dataStateInfoDialogChange.value = state
    }

    fun getData(rover: RoverMaster, date: Long) {
        job?.cancel()
        job = viewModelScope.launch {

            if (isSavedView) {
                marsRoverPhotosRepository.getLikedPhotos(rover = rover)
                    .cachedIn(viewModelScope)
                    .onEach { its ->
                        _dataStatePaging.value = Event(its)
                    }
                    .launchIn(this)
            } else {
                marsRoverPhotosRepository.getPhotos(rover = rover, date = date)
                    .cachedIn(viewModelScope)
                    .onEach { its ->
                        _dataStatePaging.value = Event(its)
                    }
                    .launchIn(this)
            }
        }
    }

    fun getDataCurrent() {
        getRover()?.peekContent?.let { master ->
            getData(master, master.max_date_in_millis)
            Timber.d("getDataCurrent $master")
        }
    }

    private fun getRoverSrcDbByName(name: String) {
        viewModelScope.launch {
            marsRoverPhotosRepository.getRoverSrcDbByName(name).collect { rover ->
                setRoverMaster(rover)
            }
        }
    }

    private fun getRover() = _dataStateRoverMaster.value

}