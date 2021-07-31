package com.akhilasdeveloper.marsroverphotos.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.akhilasdeveloper.marsroverphotos.Constants.NETWORK_TIMEOUT
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.data.DateItem
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.FieldPosition
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val utilities: Utilities
) : ViewModel() {

    private val _dataState: MutableLiveData<PagingData<MarsRoverPhotoDb>> = MutableLiveData()
    private val _dataStatePosition: MutableLiveData<Int> = MutableLiveData()
    private val _dataStateDate: MutableLiveData<PagingData<DateItem>> = MutableLiveData()

    val dataState: LiveData<PagingData<MarsRoverPhotoDb>>
        get() = _dataState

    val positionState: LiveData<Int>
        get() = _dataStatePosition

    val dateState: LiveData<PagingData<DateItem>>
        get() = _dataStateDate

    fun setPosition(position: Int){
        _dataStatePosition.value = position
    }

    fun getDateData(date: String){
        viewModelScope.launch {
            marsRoverPhotosRepository.getDates(date).cachedIn(viewModelScope)
                .onEach { its->
                    _dataStateDate.value = its
                }
                .launchIn(this)
        }
    }

    /*fun getData(
        date: String,
        api_key: String
    ) {

            viewModelScope.launch {
                val job = withTimeoutOrNull(NETWORK_TIMEOUT) {
                    marsRoverPhotosRepository.getPhotosByDate(date, api_key, utilities).cachedIn(viewModelScope)
                        .onEach { dataState ->
                            _dataState.value = dataState
                        }
                        .launchIn(this)
                }

                if (job == null) {

                }
            }

    }*/
}