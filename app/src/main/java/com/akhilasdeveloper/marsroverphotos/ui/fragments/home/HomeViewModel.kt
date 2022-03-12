package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ItemSnapshotList
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ADDING_LIKES
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import com.akhilasdeveloper.marsroverphotos.utilities.Event
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository
) : ViewModel() {

    private var _dataStateSelectedList: List<MarsRoverPhotoTable> = listOf()
    private var _dataStateSelectedPositions: List<Int> = listOf()

    private val _viewStateGetData: MutableLiveData<Event<Boolean>> = MutableLiveData()

    val viewStateGetData: LiveData<Event<Boolean>>
        get() = _viewStateGetData

    private fun setViewStateGetData() {
        _viewStateGetData.value = Event(true)
    }

    private val _viewStatePinToolBar: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateTitle: MutableLiveData<String> = MutableLiveData()
    private val _viewStateSelectedTitle: MutableLiveData<String> = MutableLiveData()
    private val _viewStateSolButtonText: MutableLiveData<String> = MutableLiveData()
    private val _viewStateScrollDateDisplayText: MutableLiveData<String> = MutableLiveData()
    private val _viewStateSolSlider: MutableLiveData<Int> = MutableLiveData()
    private val _viewStateSolSliderMax: MutableLiveData<Int> = MutableLiveData()
    private val _viewStateDateButtonText: MutableLiveData<String> = MutableLiveData()
    private val _viewStateSetFastScrollerDateVisibility: MutableLiveData<Boolean> =
        MutableLiveData()
    private val _viewStateSetFastScrollerVisibility: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateSetMainProgress: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateSetTopProgress: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateSetBottomProgress: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateNotifyItemChanged: MutableLiveData<Int> = MutableLiveData()
    private val _viewStateSetSelectMenuVisibility: MutableLiveData<Boolean> = MutableLiveData()
    private var _viewStateNavigateToDate: Boolean = false
    private var _viewStateRoverMaster: RoverMaster? = null
    private var _viewStateCurrentDate: Long? = null
    private var _viewStateSolDialogValue: Float? = null
    private val _viewStateToastMessage: MutableLiveData<Event<String>> = MutableLiveData()
    private val _viewStateClearSelectionConsent: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateRemoveLikesConsent: MutableLiveData<Boolean> = MutableLiveData()
    private val _viewStateStoragePermission: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateStoragePermission: LiveData<Boolean>
        get() = _viewStateStoragePermission

    fun setViewStateStoragePermission(isSelected: Boolean) {
        _viewStateStoragePermission.value = isSelected
    }

    val viewStateRemoveLikesConsent: LiveData<Boolean>
        get() = _viewStateRemoveLikesConsent

    fun setViewStateRemoveLikesConsent(isSelected: Boolean) {
        _viewStateRemoveLikesConsent.value = isSelected
    }

    val viewStateClearSelectionConsent: LiveData<Boolean>
        get() = _viewStateClearSelectionConsent

    fun setViewStateClearSelectionConsent(isSelected: Boolean) {
        _viewStateClearSelectionConsent.value = isSelected
    }



    val viewStateToastMessage: LiveData<Event<String>>
        get() = _viewStateToastMessage

    private fun setViewStateToastMessage(message: Event<String>) {
        _viewStateToastMessage.value = message
    }

    val viewStatePinToolBar: LiveData<Boolean>
        get() = _viewStatePinToolBar
    val viewStateTitle: LiveData<String>
        get() = _viewStateTitle
    val viewStateSelectedTitle: LiveData<String>
        get() = _viewStateSelectedTitle
    val viewStateSolButtonText: LiveData<String>
        get() = _viewStateSolButtonText
    val viewStateScrollDateDisplayText: LiveData<String>
        get() = _viewStateScrollDateDisplayText
    val viewStateSolSlider: LiveData<Int>
        get() = _viewStateSolSlider
    val viewStateSolSliderMax: LiveData<Int>
        get() = _viewStateSolSliderMax
    val viewStateDateButtonText: LiveData<String>
        get() = _viewStateDateButtonText
    val viewStateSetFastScrollerDateVisibility: LiveData<Boolean>
        get() = _viewStateSetFastScrollerDateVisibility
    val viewStateSetFastScrollerVisibility: LiveData<Boolean>
        get() = _viewStateSetFastScrollerVisibility
    val viewStateSetMainProgress: LiveData<Boolean>
        get() = _viewStateSetMainProgress
    val viewStateSetTopProgress: LiveData<Boolean>
        get() = _viewStateSetTopProgress
    val viewStateSetBottomProgress: LiveData<Boolean>
        get() = _viewStateSetBottomProgress
    val viewStateNotifyItemChanged: LiveData<Int>
        get() = _viewStateNotifyItemChanged
    val viewStateSetSelectMenuVisibility: LiveData<Boolean>
        get() = _viewStateSetSelectMenuVisibility

    private fun setViewStatePinToolBar(pinToolBar: Boolean) {
        _viewStatePinToolBar.value = pinToolBar
    }

    private fun setViewStateTitle(title: String) {
        _viewStateTitle.value = title
    }

    private fun setViewStateSelectedTitle(title: String) {
        _viewStateSelectedTitle.value = title
    }

    private fun setViewStateSolButtonText(solButtonText: String) {
        _viewStateSolButtonText.value = solButtonText
    }

    fun setViewStateScrollDateDisplayText(scrollDateDisplayText: String) {
        _viewStateScrollDateDisplayText.value = scrollDateDisplayText
    }

    private fun setViewStateSolSlider(solSlider: Int) {
        _viewStateSolSlider.value = solSlider
    }

    private fun setViewStateSolSliderMax(maxVal: Int) {
        _viewStateSolSliderMax.value = maxVal
    }

    private fun setViewStateDateButtonText(date: String) {
        _viewStateDateButtonText.value = date
    }

    fun setViewStateSetFastScrollerDateVisibility(isVisible: Boolean) {
        _viewStateSetFastScrollerDateVisibility.value = isVisible
    }

    fun setViewStateSetFastScrollerVisibility(isVisible: Boolean) {
        _viewStateSetFastScrollerVisibility.value = isVisible
    }

    fun setViewStateSetMainProgress(isLoading: Boolean) {
        _viewStateSetMainProgress.value = isLoading
    }

    fun setViewStateSetTopProgress(isLoading: Boolean) {
        _viewStateSetTopProgress.value = isLoading
    }

    fun setViewStateSetBottomProgress(isLoading: Boolean) {
        _viewStateSetBottomProgress.value = isLoading
    }

    private fun setViewStateNotifyItemChanged(position: Int) {
        _viewStateNotifyItemChanged.value = position
    }

    private fun setViewStateSetSelectMenuVisibility(isVisible: Boolean) {
        _viewStateSetSelectMenuVisibility.value = isVisible
        setViewStatePinToolBar(isVisible)
        if (!isVisible) {
            setViewStateSelectedTitle("Not Selected")
        } else {
            setViewStateSelectedTitle("Selected (${getSelectedList().size})")
        }
    }

    fun setViewStateNavigateToDate(navigate: Boolean) {
        _viewStateNavigateToDate = navigate
    }

    fun setViewStateRoverMaster(roverMaster: Event<RoverMaster>) {
        val isHandled = roverMaster.hasBeenHandled()
        roverMaster.peekContent?.let { rover ->
            roverMaster.setAsHandled()
            _viewStateRoverMaster = rover
            setViewStateTitle(rover.name)
            setViewStateDateButtonText(rover.max_date)
            setViewStateSolButtonText(rover.max_sol.toString())
            setViewStateSetFastScrollerDateVisibility(false)
            setViewStateSolSlider(rover.max_sol)
            setViewStateSolSliderMax(rover.max_sol)
            if (!isHandled) {
                setViewStateCurrentDate(rover.max_date_in_millis)
                getData()
            }
        }
    }

    fun setViewStateCurrentDate(currentDate: Long?) {
        _viewStateCurrentDate = currentDate
        _viewStateRoverMaster?.let { roverMaster ->
            currentDate?.let {
                val count = (currentDate - roverMaster.landing_date_in_millis) / MILLIS_IN_A_DAY
                setViewStateSolSlider(count.toInt())
                setViewStateDateButtonText(currentDate.formatMillisToDate())
                setViewStateSolButtonText(count.toString())
            }
        }
        setViewStateSetFastScrollerDateVisibility(false)
    }


    fun getData() {
        setViewStateGetData()
    }

    private fun addSelectedData(marsRoverPhotoTable: MarsRoverPhotoTable) {
        val dataMutable = _dataStateSelectedList.toMutableList()
        dataMutable.add(marsRoverPhotoTable)
        _dataStateSelectedList = dataMutable
    }

    private fun removeSelectedData(position: MarsRoverPhotoTable) {
        val dataMutable = _dataStateSelectedList.toMutableList()
        dataMutable.remove(position)
        _dataStateSelectedList = dataMutable
    }

    private fun addSelectedPosition(position: Int) {
        val dataMutable = _dataStateSelectedPositions.toMutableList()
        dataMutable.add(position)
        _dataStateSelectedPositions = dataMutable
    }

    private fun removeSelectedPosition(position: Int) {
        val dataMutable = _dataStateSelectedPositions.toMutableList()
        dataMutable.remove(position)
        _dataStateSelectedPositions = dataMutable
    }

    fun clearSelection() {
        _dataStateSelectedList = listOf()
        _dataStateSelectedPositions.forEach {
            setViewStateNotifyItemChanged(it)
        }
        _dataStateSelectedPositions = listOf()
        setViewStateSetSelectMenuVisibility(false)
    }

    fun setLike() {
        viewModelScope.launch {
            getSelectedList().forEach { currentData ->
                marsRoverPhotosRepository.addLike(currentData)
            }
            setViewStateToastMessage(Event(ADDING_LIKES))
            clearSelection()
        }
    }

    fun updateLike() {
        getSelectedList().forEach { currentData ->
            updateLikeDb(currentData)
        }
    }

    fun updateLikeDb(currentData: MarsRoverPhotoTable) {
        viewModelScope.launch {
            marsRoverPhotosRepository.updateLike(currentData)
        }
    }

    fun setSelection(photo: MarsRoverPhotoTable, position: Int?) {
        position?.let {
            if (getSelectedList().contains(photo)) {
                removeSelectedData(photo)
                removeSelectedPosition(position)
            } else {
                addSelectedData(photo)
                addSelectedPosition(position)
            }
            setViewStateSetSelectMenuVisibility(!isSelectedListEmpty())
            setViewStateNotifyItemChanged(position)
        }
    }

    fun validateSolText(it: String): String {
        var validated = it.filter { str -> str.isDigit() }
        if (validated.isNotEmpty()) {
            val sol = validated.toInt()
            getMaxSol()?.let { max_sol ->
                if (max_sol < sol)
                    validated = max_sol.toString()
                if (sol < 0)
                    validated = "0"
            }
        }
        return validated
    }

    private val _viewStateScrollToPosition: MutableLiveData<Event<Int>> = MutableLiveData()

    val viewStateScrollToPosition: LiveData<Event<Int>>
        get() = _viewStateScrollToPosition

    fun setViewStateScrollToPosition(position: Int) {
        _viewStateScrollToPosition.value = Event(position)
    }

    private val _viewStateShowDatePicket: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateShowDatePicket: LiveData<Boolean>
        get() = _viewStateShowDatePicket

    fun setViewStateShowDatePicket(isShowing: Boolean) {
        _viewStateShowDatePicket.value = isShowing
    }

    private val _viewStateShowSolSelected: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateShowSolSelected: LiveData<Boolean>
        get() = _viewStateShowSolSelected

    fun setViewStateShowSolSelected(isShowing: Boolean) {
        _viewStateShowSolSelected.value = isShowing
    }

    private val _viewStateShowShareSelected: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateShowShareSelected: LiveData<Boolean>
        get() = _viewStateShowShareSelected

    fun setViewStateShowShareSelected(isShowing: Boolean) {
        _viewStateShowShareSelected.value = isShowing
    }

    private val _viewStateShareAsImage: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateShareAsImage: LiveData<Boolean>
        get() = _viewStateShareAsImage

    fun setViewStateShareAsImage(isSelected: Boolean) {
        _viewStateShareAsImage.value = isSelected
    }

    private val _viewStateSaveToDevice: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSaveToDevice: LiveData<Boolean>
        get() = _viewStateSaveToDevice

    fun setViewStateSaveToDevice(isSelected: Boolean) {
        _viewStateSaveToDevice.value = isSelected
    }

    fun onDateSelected(
        date: Long,
        fetch: Boolean = false,
        snapShot: ItemSnapshotList<MarsRoverPhotoTable>
    ) {
        setViewStateCurrentDate(date)
        val search = snapShot.filter { photo ->
            photo?.earth_date == date && photo.is_placeholder
        }
        if (search.isNotEmpty()) {
            val pos = snapShot.indexOf(search[0])
            setViewStateScrollToPosition(pos)
        } else if (fetch) {
            setViewStateNavigateToDate(true)
            getData()
        }
    }

    fun setSolSelectDialogValue(value: Float?) {
        _viewStateSolDialogValue = value
    }

    fun isNavigateToDate(): Boolean = _viewStateNavigateToDate
    fun isSelectedListEmpty(): Boolean = _dataStateSelectedList.isNullOrEmpty()
    fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean =
        _dataStateSelectedList.contains(marsRoverPhotoTable)

    fun getSelectedList() = _dataStateSelectedList
    fun getSelectedItemPosition(position: Int) = _dataStateSelectedPositions[position]
    fun getCurrentDate() = _viewStateCurrentDate
    fun getLandingDateInMillis() = _viewStateRoverMaster?.landing_date_in_millis
    fun getMaxDateInMillis() = _viewStateRoverMaster?.max_date_in_millis
    fun getMaxSol() = _viewStateRoverMaster?.max_sol
    fun getRover() = _viewStateRoverMaster
    fun getSolSelectDialogValue() = _viewStateSolDialogValue

}