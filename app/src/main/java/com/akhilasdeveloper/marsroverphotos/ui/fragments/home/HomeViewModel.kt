package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository
) : ViewModel() {

    private val _viewStatePinToolBar: MutableLiveData<Boolean> = MutableLiveData()

    val viewStatePinToolBar: LiveData<Boolean>
        get() = _viewStatePinToolBar

    fun setViewStatePinToolBar(pinToolBar: Boolean) {
        _viewStatePinToolBar.value = pinToolBar
    }

    private val _viewStateTitle: MutableLiveData<String> = MutableLiveData()

    val viewStateTitle: LiveData<String>
        get() = _viewStateTitle

    fun setViewStateTitle(title: String) {
        _viewStateTitle.value = title
    }

    private val _viewStateSolButtonText: MutableLiveData<String> = MutableLiveData()

    val viewStateSolButtonText: LiveData<String>
        get() = _viewStateSolButtonText

    fun setViewStateSolButtonText(solButtonText: String) {
        _viewStateSolButtonText.value = solButtonText
    }

    private val _viewStateRoverMaster: MutableLiveData<RoverMaster> = MutableLiveData()

    val viewStateRoverMaster: LiveData<RoverMaster>
        get() = _viewStateRoverMaster

    fun setViewStateRoverMaster(roverMaster: RoverMaster) {
        _viewStateRoverMaster.value = roverMaster
    }


}