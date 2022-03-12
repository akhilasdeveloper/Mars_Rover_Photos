package com.akhilasdeveloper.marsroverphotos.ui.fragments.rovers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.utilities.Event
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoversViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository
) : ViewModel() {

    private var job: Job? = null

    //ViewState
    private val _viewStateRoverSwipeRefresh: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateSetEmptyMessage: MutableLiveData<Event<String>?> = MutableLiveData()
    private val _viewStateRoverMasterList: MutableLiveData<List<RoverMaster>> = MutableLiveData()
    private val _viewStateErrorMessage: MutableLiveData<Event<String>> = MutableLiveData()
    private val _viewStateMessage: MutableLiveData<Event<String>> = MutableLiveData()
    private val _viewStateSheetData: MutableLiveData<RoverMaster> = MutableLiveData()
    private val _viewStateSheetState: MutableLiveData<Int> = MutableLiveData()
    private val _viewStateTopBarVisibility: MutableLiveData<Boolean> = MutableLiveData()

    //DataState
    private val _dataStateRover: MutableLiveData<Event<MarsRoverSrcResponse>> = MutableLiveData()

    val viewStateRoverSwipeRefresh: LiveData<Boolean>
        get() = _viewStateRoverSwipeRefresh
    val viewStateSetEmptyMessage: LiveData<Event<String>?>
        get() = _viewStateSetEmptyMessage
    val viewStateRoverMasterList: LiveData<List<RoverMaster>>
        get() = _viewStateRoverMasterList
    val viewStateErrorMessage: LiveData<Event<String>>
        get() = _viewStateErrorMessage
    val viewStateMessage: LiveData<Event<String>>
        get() = _viewStateMessage
    val viewStateSheetData: LiveData<RoverMaster>
        get() = _viewStateSheetData
    val viewStateSheetState: LiveData<Int>
        get() = _viewStateSheetState
    val viewStateTopBarVisibility: LiveData<Boolean>
        get() = _viewStateTopBarVisibility

    val dataStateRover: LiveData<Event<MarsRoverSrcResponse>>
        get() = _dataStateRover

    private fun setViewStateRoverSwipeRefresh(isRefreshing:Boolean){
        _viewStateRoverSwipeRefresh.value = isRefreshing
    }

    private fun setViewStateSetEmptyMessage(message: String?){
        _viewStateSetEmptyMessage.value = if (message==null) null else Event(message)
    }

    private fun setViewStateRoverMasterList(list: List<RoverMaster>){
        _viewStateRoverMasterList.value = list
    }

    private fun setViewStateErrorMessage(message: String){
        _viewStateErrorMessage.value = Event(message)
    }

    private fun setViewStateMessage(message: String){
        _viewStateMessage.value = Event(message)
    }

    fun setViewStateSheetData(roverMaster: RoverMaster){
        _viewStateSheetData.value = roverMaster
    }

    fun setViewStateSheetState(state: Int){
        _viewStateSheetState.value = state
        setViewStateTopBarVisibility(state == BottomSheetBehavior.STATE_COLLAPSED)
    }

    private fun setViewStateTopBarVisibility(isVisible:Boolean){
        _viewStateTopBarVisibility.value = isVisible
    }

    fun getRoverData(isRefresh: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            marsRoverPhotosRepository.getRoverData(isRefresh).collect {marsRoverSrcResponse->
                _dataStateRover.value = Event(marsRoverSrcResponse)

                marsRoverSrcResponse.let { response ->
                    response.data?.let {roverMasterList->
                        if (roverMasterList.isEmpty())
                            setViewStateSetEmptyMessage("")
                        else {
                            setViewStateSetEmptyMessage(null)
                            setViewStateRoverMasterList(roverMasterList)
                        }
                    }

                    response.message?.let {message->
                        setViewStateMessage(message)
                    }

                    setViewStateRoverSwipeRefresh(response.isLoading)

                    response.error?.let {
                        setViewStateErrorMessage(it)
                        setViewStateSetEmptyMessage("")
                    }
                }
            }
        }
    }
}