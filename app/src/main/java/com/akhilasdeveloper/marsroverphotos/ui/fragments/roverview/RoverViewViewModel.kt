package com.akhilasdeveloper.marsroverphotos.ui.fragments.roverview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoverViewViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository
) : ViewModel() {

    private val _dataStateIsLiked: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateCurrentData: MutableLiveData<MarsRoverPhotoTable> = MutableLiveData()
    private val _viewStateShowInfoDialog: MutableLiveData<MarsRoverPhotoTable?> = MutableLiveData()
    private val _viewStateShowMoreSelectDialog: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateStorageConsentDialog: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateLikeConsentDialog: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateLikeConsentDialog: LiveData<Boolean>
        get() = _viewStateLikeConsentDialog

    fun setViewStateLikeConsentDialog(isSelected: Boolean) {
        _viewStateLikeConsentDialog.value = isSelected
    }

    val viewStateStorageConsentDialog: LiveData<Boolean>
        get() = _viewStateStorageConsentDialog

    fun setViewStateStorageConsentDialog(isSelected: Boolean) {
        _viewStateStorageConsentDialog.value = isSelected
    }



    val viewStateShowMoreSelectDialog: LiveData<Boolean>
        get() = _viewStateShowMoreSelectDialog

    fun setViewStateShowMoreSelectDialog(isSelected: Boolean) {
        _viewStateShowMoreSelectDialog.value = isSelected
    }


    val viewStateShowInfoDialog: LiveData<MarsRoverPhotoTable?>
        get() = _viewStateShowInfoDialog

    fun setViewStateShowInfoDialog(currentData: MarsRoverPhotoTable? = getCurrentData) {
        _viewStateShowInfoDialog.value = currentData
    }

    val viewStateCurrentData: LiveData<MarsRoverPhotoTable>
        get() = _viewStateCurrentData

    fun setViewStateCurrentData(currentData: MarsRoverPhotoTable) {
        _viewStateCurrentData.value = currentData
        getIsLiked()
    }

    val dataStateIsLiked: LiveData<Boolean>
        get() = _dataStateIsLiked

    private fun getIsLiked() {
        getCurrentDataPhotoID?.let { id ->
            isLiked(id)
        }
    }

    fun setLike() {
        getCurrentData?.let { photo ->
            updateLike(
                marsRoverPhotoTable = photo
            )
        }
    }

    private fun isLiked(id: Long) {
        viewModelScope.launch {
            marsRoverPhotosRepository.isLiked(id).collect {
                _dataStateIsLiked.value = it
            }
        }
    }

    fun updateCurrentLike(){
        getCurrentData?.let { currentData ->
            updateLike(
                marsRoverPhotoTable = currentData
            )
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

    val getCurrentData
        get() = _viewStateCurrentData.value
    val getCurrentDataImage
        get() = getCurrentData?.img_src
    val getCurrentDataRoverName
        get() = getCurrentData?.rover_name
    val getCurrentDataCameraName
        get() = getCurrentData?.camera_name
    val getCurrentDataPhotoID
        get() = getCurrentData?.photo_id
    val getCurrentDataEarthDate
        get() = getCurrentData?.earth_date

}