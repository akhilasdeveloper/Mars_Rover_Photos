package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.repositories.MarsRoverPhotosRepository
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import com.akhilasdeveloper.marsroverphotos.utilities.Event
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDate
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val marsRoverPhotosRepository: MarsRoverPhotosRepository,
    private val utilities: Utilities
) : ViewModel() {

    private val _viewStatePinToolBar: MutableLiveData<Boolean> = MutableLiveData()

    val viewStatePinToolBar: LiveData<Boolean>
        get() = _viewStatePinToolBar

    private fun setViewStatePinToolBar(pinToolBar: Boolean) {
        _viewStatePinToolBar.value = pinToolBar
    }

    private val _viewStateTitle: MutableLiveData<String> = MutableLiveData()

    val viewStateTitle: LiveData<String>
        get() = _viewStateTitle

    private fun setViewStateTitle(title: String) {
        _viewStateTitle.value = title
    }

    private val _viewStateSelectedTitle: MutableLiveData<String> = MutableLiveData()

    val viewStateSelectedTitle: LiveData<String>
        get() = _viewStateSelectedTitle

    private fun setViewStateSelectedTitle(title: String) {
        _viewStateSelectedTitle.value = title
    }

    private val _viewStateSolButtonText: MutableLiveData<String> = MutableLiveData()

    val viewStateSolButtonText: LiveData<String>
        get() = _viewStateSolButtonText

    private fun setViewStateSolButtonText(solButtonText: String) {
        _viewStateSolButtonText.value = solButtonText
    }

    private val _viewStateRoverMaster: MutableLiveData<RoverMaster> = MutableLiveData()

    val viewStateRoverMaster: LiveData<RoverMaster>
        get() = _viewStateRoverMaster

    fun setViewStateRoverMaster(roverMaster: Event<RoverMaster>) {
        val isHandled = roverMaster.hasBeenHandled()
        roverMaster.peekContent?.let { rover ->
            roverMaster.setAsHandled()
            _viewStateRoverMaster.value = rover
            setViewStateTitle(rover.name)
            setViewStateDateButtonText(rover.max_date)
            setViewStateSolButtonText(rover.max_sol.toString())
            setViewStateSetFastScrollerDateVisibility(false)
            setViewStateSolSlider(rover.max_sol)
            setViewStateSolSliderMax(rover.max_sol)
            if (!isHandled) {
                setViewStateCurrentDate(rover.max_date_in_millis)
                getData(date = rover.max_date_in_millis, rover = rover)
            }
        }
    }

    private val _viewStateCurrentDate: MutableLiveData<Long> = MutableLiveData()

    val viewStateCurrentDate: LiveData<Long>
        get() = _viewStateCurrentDate

    fun setViewStateCurrentDate(currentDate: Long) {
        _viewStateCurrentDate.value = currentDate
        _viewStateRoverMaster.value?.let { roverMaster ->
            val count = (currentDate - roverMaster.landing_date_in_millis) / MILLIS_IN_A_DAY
            setViewStateSolSlider(count.toInt())
            setViewStateDateButtonText(currentDate.formatMillisToDate())
            setViewStateSolButtonText(count.toString())
        }
        setViewStateSetFastScrollerDateVisibility(false)
    }

    private val _viewStateScrollDateDisplayText: MutableLiveData<String> = MutableLiveData()

    val viewStateScrollDateDisplayText: LiveData<String>
        get() = _viewStateScrollDateDisplayText

    fun setViewStateScrollDateDisplayText(scrollDateDisplayText: String) {
        _viewStateScrollDateDisplayText.value = scrollDateDisplayText
    }

    private val _viewStateSolSlider: MutableLiveData<Int> = MutableLiveData()

    val viewStateSolSlider: LiveData<Int>
        get() = _viewStateSolSlider

    private fun setViewStateSolSlider(solSlider: Int) {
        _viewStateSolSlider.value = solSlider
    }

    private val _viewStateSolSliderMax: MutableLiveData<Int> = MutableLiveData()

    val viewStateSolSliderMax: LiveData<Int>
        get() = _viewStateSolSliderMax

    private fun setViewStateSolSliderMax(maxVal: Int) {
        _viewStateSolSliderMax.value = maxVal
    }

    private val _viewStateDateButtonText: MutableLiveData<String> = MutableLiveData()

    val viewStateDateButtonText: LiveData<String>
        get() = _viewStateDateButtonText

    private fun setViewStateDateButtonText(date: String) {
        _viewStateDateButtonText.value = date
    }

    private val _viewStateSetFastScrollerDateVisibility: MutableLiveData<Boolean> =
        MutableLiveData()

    val viewStateSetFastScrollerDateVisibility: LiveData<Boolean>
        get() = _viewStateSetFastScrollerDateVisibility

    fun setViewStateSetFastScrollerDateVisibility(isVisible: Boolean) {
        _viewStateSetFastScrollerDateVisibility.value = isVisible
    }

    private val _viewStateSetFastScrollerVisibility: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSetFastScrollerVisibility: LiveData<Boolean>
        get() = _viewStateSetFastScrollerVisibility

    fun setViewStateSetFastScrollerVisibility(isVisible: Boolean) {
        _viewStateSetFastScrollerVisibility.value = isVisible
    }

    private val _viewStateSetMainProgress: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSetMainProgress: LiveData<Boolean>
        get() = _viewStateSetMainProgress

    fun setViewStateSetMainProgress(isLoading: Boolean) {
        _viewStateSetMainProgress.value = isLoading
    }

    private val _viewStateSetTopProgress: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSetTopProgress: LiveData<Boolean>
        get() = _viewStateSetTopProgress

    fun setViewStateSetTopProgress(isLoading: Boolean) {
        _viewStateSetTopProgress.value = isLoading
    }

    private val _viewStateSetBottomProgress: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSetBottomProgress: LiveData<Boolean>
        get() = _viewStateSetBottomProgress

    fun setViewStateSetBottomProgress(isLoading: Boolean) {
        _viewStateSetBottomProgress.value = isLoading
    }

    private val _viewStateNotifyItemChanged: MutableLiveData<Int> = MutableLiveData()

    val viewStateNotifyItemChanged: LiveData<Int>
        get() = _viewStateNotifyItemChanged

    private fun setViewStateNotifyItemChanged(position: Int) {
        _viewStateNotifyItemChanged.value = position
    }

    private val _viewStateSetSelectMenuVisibility: MutableLiveData<Boolean> = MutableLiveData()

    val viewStateSetSelectMenuVisibility: LiveData<Boolean>
        get() = _viewStateSetSelectMenuVisibility

    private fun setViewStateSetSelectMenuVisibility(isVisible: Boolean) {
        _viewStateSetSelectMenuVisibility.value = isVisible
        setViewStatePinToolBar(isVisible)
        if (!isVisible){
            setViewStateSelectedTitle("Not Selected")
        }else{
            setViewStateSelectedTitle("Selected (${getSelectedList().size})")
        }
    }

    private var job: Job? = null

    private val _dataStatePaging: MutableLiveData<Event<PagingData<MarsRoverPhotoTable>?>> = MutableLiveData()
    private val _dataStateSelectedList: MutableLiveData<List<MarsRoverPhotoTable>> = MutableLiveData()
    private val _dataStateSelectedPositions: MutableLiveData<List<Int>> = MutableLiveData()

    val dataStatePaging: LiveData<Event<PagingData<MarsRoverPhotoTable>?>>
        get() = _dataStatePaging

    val dataStateSelectedList: LiveData<List<MarsRoverPhotoTable>>
        get() = _dataStateSelectedList

    val dataStateSelectedPositions: LiveData<List<Int>>
        get() = _dataStateSelectedPositions

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

    fun addSelectedData(marsRoverPhotoTable: MarsRoverPhotoTable){
        val data = dataStateSelectedList.value ?: mutableListOf()
        val dataMutable = data.toMutableList()
        dataMutable.add(marsRoverPhotoTable)
        _dataStateSelectedList.value = dataMutable
    }

    fun removeSelectedData(position: MarsRoverPhotoTable){
        val data = dataStateSelectedList.value ?: mutableListOf()
        val dataMutable = data.toMutableList()
        dataMutable.remove(position)
        _dataStateSelectedList.value = dataMutable
    }

    fun addSelectedPosition(position: Int){
        val data = dataStateSelectedPositions.value ?: mutableListOf()
        val dataMutable = data.toMutableList()
        dataMutable.add(position)
        _dataStateSelectedPositions.value = dataMutable
    }

    fun removeSelectedPosition(position: Int){
        val data = dataStateSelectedPositions.value ?: mutableListOf()
        val dataMutable = data.toMutableList()
        dataMutable.remove(position)
        _dataStateSelectedPositions.value = dataMutable
    }

    fun clearSelection(){
        _dataStateSelectedList.value = listOf()
        getSelectedItemPositions().forEach {
            setViewStateNotifyItemChanged(it)
        }
        _dataStateSelectedPositions.value = listOf()
        setViewStateSetSelectMenuVisibility(false)
    }

    fun setLike() {
        getSelectedList().forEach { currentData ->
            addLike(
                marsRoverPhotoTable = currentData
            )
        }
        clearSelection()
//        requireContext().showShortToast("Added to liked photos")
    }

    private fun addLike(
        marsRoverPhotoTable: MarsRoverPhotoTable
    ) {
        viewModelScope.launch {
            marsRoverPhotosRepository.addLike(marsRoverPhotoTable)
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

    fun isSelectedListEmpty():Boolean = dataStateSelectedList.value.isNullOrEmpty()
    fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable):Boolean = dataStateSelectedList.value?.contains(marsRoverPhotoTable)?:false
    fun getSelectedList() = dataStateSelectedList.value?: listOf()
    fun getSelectedItemPositions() = dataStateSelectedPositions.value?: listOf()
    fun getSelectedItemPosition(position: Int) = dataStateSelectedPositions.value?.get(position)
}