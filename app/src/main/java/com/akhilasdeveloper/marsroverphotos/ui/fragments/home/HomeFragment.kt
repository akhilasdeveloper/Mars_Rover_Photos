package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.SelectionChecker
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_LANDSCAPE_LARGE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_LANDSCAPE_NORMAL
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_LANDSCAPE_SMALL
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_LANDSCAPE_X_LARGE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_LARGE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_NORMAL
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_SMALL
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN_X_LARGE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_SOL
import com.bumptech.glide.RequestManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    internal val binding get() = _binding!!
    @Inject
    lateinit var requestManager: RequestManager
    lateinit var homeViewModel: HomeViewModel
    private var adapter: MarsRoverPhotoAdapter? = null
    private var hideFastScrollerJob: Job? = null
    private var downloadJob: Job? = null
    private var saveJob: Job? = null
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var utilities: Utilities

    override fun onCreate(savedInstanceState: Bundle?) {

        setBackPressCallBack()
        super.onCreate(savedInstanceState)
    }

    private fun setBackPressCallBack() {
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    if (!homeViewModel.isSelectedListEmpty()) {
                        homeViewModel.setViewStateClearSelectionConsent(true)
                    } else
                        if (isEnabled) {
                            isEnabled = false
                            requireActivity().onBackPressed()
                        }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
        uiObservers()
    }

    private fun uiObservers() {
        homeViewModel.apply {
            viewStatePinToolBar.observe(viewLifecycleOwner, {
                binding.homeBottomToolbarSecond.isVisible = it
            })
            viewStateTitle.observe(viewLifecycleOwner, {
                (if (viewModel.isSavedView) getString(
                    R.string.liked_photos,
                    it
                ) else it).let { title ->
                    binding.topAppbar.homeToolbarTop.title = title
                    binding.topAppbar.homeCollapsingToolbarTop.title = title
                }
            })
            viewStateSelectedTitle.observe(viewLifecycleOwner, {
                binding.homeBottomToolbarSecond.title = getString(R.string.selected, getSelectedList().size.toString())
            })
            viewStateSolButtonText.observe(viewLifecycleOwner, { solButtonText ->
                binding.bottomAppbar.solButtonText.text = getString(
                    R.string.sol, solButtonText
                )
            })
            viewStateScrollDateDisplayText.observe(viewLifecycleOwner, { scrollDateDisplayText ->
                binding.scrollDateDisplayText.text = scrollDateDisplayText
            })
            viewStateSolSlider.observe(viewLifecycleOwner, { solSlider ->
                binding.solSlider.progress = solSlider
            })
            viewStateSolSliderMax.observe(viewLifecycleOwner, { maxVal ->
                binding.solSlider.max = maxVal
            })
            viewStateDateButtonText.observe(viewLifecycleOwner, { date ->
                binding.bottomAppbar.dateButtonText.text = date
            })
            viewStateSetMainProgress.observe(viewLifecycleOwner, { isLoading ->
                if (isLoading) uiCommunicationListener.showIndeterminateProgressDialog()
                else
                    uiCommunicationListener.hideIndeterminateProgressDialog()

            })
            viewStateSetBottomProgress.observe(viewLifecycleOwner, { isLoading ->
                binding.progress.isVisible = isLoading
            })

            viewStateSetTopProgress.observe(viewLifecycleOwner, { isLoading ->
                binding.progressTop.isVisible = isLoading
            })
            viewStateSetFastScrollerDateVisibility.observe(viewLifecycleOwner, { isVisible ->
                if (isVisible)
                    showFastScrollerDate()
                else
                    hideFastScrollerDate()
            })

            viewStateSetFastScrollerVisibility.observe(viewLifecycleOwner, { isVisible ->
                if (isVisible)
                    showFastScroller()
                else
                    hideFastScroller()

            })
            viewStateNotifyItemChanged.observe(viewLifecycleOwner, { position ->
                adapter?.notifyItemChanged(position)
            })
            viewStateSetSelectMenuVisibility.observe(viewLifecycleOwner, { isVisible ->
                if (isVisible)
                    populateSelectMenu()
                else
                    clearSelectMenu()
            })

            viewStateScrollToPosition.observe(viewLifecycleOwner, { position ->
                position.contentIfNotHandled?.let { it ->
                    scrollToPosition(it)
                }
            })
            viewStateShowDatePicket.observe(viewLifecycleOwner, { isShowing ->
                if (isShowing)
                    showDatePicker()
            })

            viewStateShowSolSelected.observe(viewLifecycleOwner, { isShowing ->
                if (isShowing)
                    showSolSelectorDialog()
            })

            viewStateShowShareSelected.observe(viewLifecycleOwner, { isShowing ->
                if (isShowing)
                    uiCommunicationListener.showMoreSelectorDialog(
                        onImageSelect = {
                            homeViewModel.setViewStateShareAsImage(true)
                        },
                        onLinkSelect = {
                            shareAllAsLinks()
                        },
                        onDownloadSelect = {
                            updateOrRequestPermission()
                        },
                        items = homeViewModel.getSelectedList(),
                        onDeleteSelect = { photo, position ->
                            homeViewModel.setSelection(
                                photo,
                                homeViewModel.getSelectedItemPosition(position)
                            )
                        }, onDismiss = {
                            homeViewModel.setViewStateShowShareSelected(false)
                        })
            })

            viewStateShareAsImage.observe(viewLifecycleOwner, { isSelected ->
                if (isSelected)
                    shareAllAsImage()
            })

            viewStateSaveToDevice.observe(viewLifecycleOwner, { isSelected ->
                if (isSelected)
                    setDownload(homeViewModel.getSelectedList())
            })

            viewStateGetData.observe(viewLifecycleOwner, { load ->
                load?.contentIfNotHandled?.let {
                    getCurrentDate()?.let { currentDate ->
                        getRover()?.let { master ->
                            viewModel.getData(master, currentDate)
                        }
                    }
                }
            })
            viewStateToastMessage.observe(viewLifecycleOwner, { message ->
                message?.contentIfNotHandled?.let {
                    requireContext().showShortToast(it)
                }
            })
            viewStateClearSelectionConsent.observe(viewLifecycleOwner, { isSelected ->
                if (isSelected) {
                    uiCommunicationListener.showConsentSelectorDialog(getString(R.string.clear_selection),
                        getString(
                            R.string.clear_selection_consent
                        ),
                        onOkSelect = {
                            homeViewModel.clearSelection()
                        }, onDismiss = {
                            setViewStateClearSelectionConsent(false)
                        })
                }
            })
            viewStateRemoveLikesConsent.observe(viewLifecycleOwner, { isSelected ->
                if (isSelected) {
                    uiCommunicationListener.showConsentSelectorDialog(getString(R.string.remove_from_liked_photos),
                        getString(
                            R.string.selected_items_will_be_removed
                        ),
                        doNotShow = true,
                        onOkSelect = {
                            if (it) {
                                lifecycleScope.launch {
                                    utilities.setHideLikesConsent()
                                }
                            }
                            updateLike()
                        }, onCancelSelect = {
                            if (it) {
                                lifecycleScope.launch {
                                    utilities.setHideLikesConsent()
                                }
                            }
                        }, onDismiss = {
                            setViewStateRemoveLikesConsent(false)
                        })
                }
            })
            viewStateStoragePermission.observe(viewLifecycleOwner, { isSelected ->
                if (isSelected) {
                    uiCommunicationListener.showConsentSelectorDialog(
                        title = requireContext().getString(R.string.permission),
                        descriptionText = requireContext().getString(R.string.permission_consent),
                        onOkSelect = {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        },
                        onCancelSelect = {
                            uiCommunicationListener.showSnackBarMessage(
                                messageText = requireContext().getString(
                                    R.string.permission_cancel
                                ),
                                buttonText = requireContext().getString(R.string.allow),
                                onClick = {
                                    updateOrRequestPermission()
                                })
                        }, onDismiss = {
                            setViewStateStoragePermission(false)
                        })
                }
            })
        }
    }

    private fun subscribeObservers() {

        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            homeViewModel.setViewStateRoverMaster(it)
        })

        viewModel.positionState.observe(viewLifecycleOwner, {
            it.contentIfNotHandled?.let { position ->
                homeViewModel.setViewStateScrollToPosition(position)
            }
        })

        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
            it?.let {
                val isHandled = it.hasBeenHandled()
                it.peekContent?.let { photos ->
                    it.setAsHandled()
                    adapter?.submitData(viewLifecycleOwner.lifecycle, photos)
                    if (!viewModel.isSavedView) {
                        if (!isHandled) {
                            homeViewModel.getCurrentDate()?.let { currentDate ->
                                onDateSelected(currentDate)
                            }
                        }
                    }
                }
            }
        })

    }

    private fun init() {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        setWindowInsets()
        adapter = MarsRoverPhotoAdapter(this, requestManager, viewModel.isSavedView)

        if (viewModel.isSavedView) {
            val layoutManager = LinearLayoutManager(requireContext())
            binding.photoRecycler.layoutManager = layoutManager
            hideHomeContent()
        } else {
            val layoutManager = GridLayoutManager(
                requireContext(),
                getGallerySpan(),
                GridLayoutManager.VERTICAL,
                false
            )
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    adapter?.let { adapter ->
                        return if (adapter.snapshot().size > position)
                            if (adapter.snapshot()[position]?.is_placeholder != true)
                                1
                            else
                                getGallerySpan()
                        else
                            getGallerySpan()
                    } ?: return getGallerySpan()
                }
            }

            binding.photoRecycler.layoutManager = layoutManager
            hideSavedContent()
            homeViewModel.setViewStateSetFastScrollerVisibility(false)
        }

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.adapter = adapter
        }

        binding.homeBottomToolbarSecond.setNavigationOnClickListener {
            homeViewModel.clearSelection()
        }

        binding.homeBottomToolbarSecond.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.share -> {
                    homeViewModel.setViewStateShowShareSelected(true)
                    true
                }
                R.id.favorites -> {
                    if (viewModel.isSavedView)
                        setSavedLike()
                    else
                        homeViewModel.setLike()
                    true
                }
                R.id.wallpaper -> {
                    if (!homeViewModel.isSelectedListEmpty())
                        setWallpaper(homeViewModel.getSelectedList()[0])
                    true
                }
                else -> false
            }
        }

        adapter?.selectionChecker = object : SelectionChecker {
            override fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean =
                homeViewModel.isSelected(marsRoverPhotoTable)
        }

        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.getBitmap(requireContext())?.let { bitmap ->
                    val wallpaperManager =
                        WallpaperManager.getInstance(requireActivity().applicationContext)
                    lifecycleScope.launch {
                        wallpaperManager.setBitmap(bitmap)
                        homeViewModel.clearSelection()
                        requireContext().showShortToast(message = getString(R.string.wallpapet_set))
                    }
                }
            }
        }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                writePermissionGranted = it
                homeViewModel.setViewStateSaveToDevice(true)
            }
    }

    private fun setSavedLike() {
        lifecycleScope.launch {
            if (utilities.isShowLikesConsent()) {
                homeViewModel.setViewStateRemoveLikesConsent(true)
            } else {
                updateLike()
            }
        }
    }

    private fun updateLike() {
        homeViewModel.updateLike()
        val list: ArrayList<MarsRoverPhotoTable> = arrayListOf()
        list.addAll(homeViewModel.getSelectedList())
        homeViewModel.clearSelection()
        uiCommunicationListener.showSnackBarMessage(
            getString(R.string.items_removed_from_like),
            getString(R.string.undo),
            onClick = {
                list.forEach { currentData ->
                    currentData.let {
                        homeViewModel.updateLikeDb(
                            currentData = currentData
                        )
                    }
                }
                requireContext().showShortToast(getString(R.string.photos_added_back))
            })
    }

    private fun hideSavedContent() {
        val homeBottomToolbarSecondLayoutParams =
            binding.homeBottomToolbarSecond.layoutParams as CoordinatorLayout.LayoutParams
        homeBottomToolbarSecondLayoutParams.anchorId = R.id.bottom_appbar
        homeBottomToolbarSecondLayoutParams.anchorGravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        homeBottomToolbarSecondLayoutParams.gravity = Gravity.CENTER or Gravity.TOP
        binding.homeBottomToolbarSecond.layoutParams = homeBottomToolbarSecondLayoutParams
    }

    private fun hideHomeContent() {
        binding.apply {
            bottomAppbar.root.isVisible = false
            scrollDateDisplayText.isVisible = false
            slideFrame.isVisible = false
            val homeBottomToolbarSecondLayoutParams =
                homeBottomToolbarSecond.layoutParams as CoordinatorLayout.LayoutParams
            homeBottomToolbarSecondLayoutParams.gravity = Gravity.BOTTOM
            homeBottomToolbarSecond.layoutParams = homeBottomToolbarSecondLayoutParams
        }
    }

    private fun getGallerySpan(): Int =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            when (screenSize) {
                Configuration.SCREENLAYOUT_SIZE_LARGE -> GALLERY_SPAN_LANDSCAPE_LARGE
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> GALLERY_SPAN_LANDSCAPE_NORMAL
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> GALLERY_SPAN_LANDSCAPE_X_LARGE
                Configuration.SCREENLAYOUT_SIZE_SMALL -> GALLERY_SPAN_LANDSCAPE_SMALL
                else -> {
                    GALLERY_SPAN_NORMAL
                }
            }
        } else {
            when (screenSize) {
                Configuration.SCREENLAYOUT_SIZE_LARGE -> GALLERY_SPAN_LARGE
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> GALLERY_SPAN_NORMAL
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> GALLERY_SPAN_X_LARGE
                Configuration.SCREENLAYOUT_SIZE_SMALL -> GALLERY_SPAN_SMALL
                else -> {
                    GALLERY_SPAN_NORMAL
                }
            }
        }

    private fun updateOrRequestPermission() {
        val hasWritePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        writePermissionGranted = hasWritePermission || minSdk29

        if (!writePermissionGranted) {
            homeViewModel.setViewStateStoragePermission(true)
        } else {
            homeViewModel.setViewStateSaveToDevice(true)
        }
    }

    private fun setDownload(selectedList: List<MarsRoverPhotoTable>) {
        if (writePermissionGranted) {
            downloadJob?.cancel()
            downloadJob = CoroutineScope(Dispatchers.IO).launch {
                selectedList.forEachIndexed { index, currentData ->
                    download(currentData)
                    withContext(Dispatchers.Main) {
                        uiCommunicationListener.showDownloadProgressDialog(
                            ((index.plus(1)
                                .toFloat() / selectedList.size.toFloat()) * 100).toInt()
                        ) {
                            uiCommunicationListener.hideDownloadProgressDialog()
                            homeViewModel.setViewStateSaveToDevice(false)
                            downloadJob?.cancel()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    uiCommunicationListener.showSnackBarMessage(
                        getString(R.string.images_saved_to_gallery),
                    )
                    uiCommunicationListener.hideDownloadProgressDialog()
                    homeViewModel.setViewStateSaveToDevice(false)
                }
            }
        } else {
            homeViewModel.setViewStateSaveToDevice(false)
        }
    }

    private fun download(currentData: MarsRoverPhotoTable): Uri? {
        var uri: Uri? = null
        currentData.img_src.downloadImageAsBitmap(requireContext()) { image ->
            image?.let {
                uri = savePhotoToExternalStorage(utilities.getDisplayName(currentData), it)
            }
        }
        return uri
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Uri? {
        val imageCollection = sdk29andUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }

        try {
            requireActivity().contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream))
                        throw IOException("Failed to save Image!")
                    return uri
                }
            } ?: throw IOException("Couldn't Create MediaStore Entry")
            return null
        } catch (exception: IOException) {
            Timber.e(exception.fillInStackTrace())
            return null
        }
    }

    private fun setWallpaper(marsRoverPhoto: MarsRoverPhotoTable) {
        marsRoverPhoto.img_src.downloadImageAsUri(requestManager) { uri ->
            uri?.let {
                cropImage.launch(
                    options(uri = it) {
                        setGuidelines(CropImageView.Guidelines.ON)
                        setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                    }
                )
            }
        }

    }

    private fun shareAllAsLinks() {
        if (!homeViewModel.isSelectedListEmpty()) {
            var links = ""
            homeViewModel.getSelectedList().forEach {
                links += "${it.img_src} \n"
            }
            homeViewModel.getSelectedList()[0].let {
                val intent = Intent(Intent.ACTION_SEND)
                val shareBody = resources.getString(
                    if (homeViewModel.getSelectedList().size == 1) R.string.share_text else R.string.share_texts,
                    it.rover_name,
                    links
                )
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, shareBody)
                startActivity(Intent.createChooser(intent, shareBody))
            }
        }
    }

    private fun shareAllAsImage() {
        downloadJob?.cancel()
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            downloadImage().let {
                withContext(Dispatchers.Main) {
                    homeViewModel.setViewStateShareAsImage(false)
                }
                if (it.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                    intent.type = "image/png"
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, it)
                    startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
                }
                withContext(Dispatchers.Main) {
                    uiCommunicationListener.hideDownloadProgressDialog()
                }
            }
        }
    }

    private suspend fun downloadImage(selectedList: List<MarsRoverPhotoTable> = homeViewModel.getSelectedList()): ArrayList<Uri> {
        val list: ArrayList<Uri> = arrayListOf()
        val size = selectedList.size
        selectedList.forEachIndexed { index, photo ->
            withContext(Dispatchers.Main) {
                uiCommunicationListener.showDownloadProgressDialog(
                    ((index.plus(1).toFloat() / size.toFloat()) * 100).toInt()
                ) {
                    uiCommunicationListener.hideDownloadProgressDialog()
                    downloadJob?.cancel()
                }
            }
            val displayName = utilities.getDisplayName(photo)

            if (utilities.isFileExistInCache(displayName)) {
                utilities.toImageUriFromName(displayName)
            } else {
                photo.img_src.downloadImageAsBitmap2(requestManager)?.let {
                    utilities.toImageURI(it, displayName)
                }
            }?.let { uri ->
                list.add(uri)
            }
        }
        return list
    }


    private fun clearSelectMenu() {
        if (binding.homeBottomToolbarSecond.menu.isNotEmpty()) {
            binding.homeBottomToolbarSecond.menu.clear()
        }
    }

    private fun populateSelectMenu() {
        val menu = if (homeViewModel.getSelectedList().size == 1)
            R.menu.top_appbar_single_item_select_menu
        else
            R.menu.top_appbar_select_menu
        val menuSize = if (homeViewModel.getSelectedList().size == 1) 3 else 2
        if (binding.homeBottomToolbarSecond.menu.isEmpty() ||
            binding.homeBottomToolbarSecond.menu.size() != menuSize
        ) {
            clearSelectMenu()
            binding.homeBottomToolbarSecond.inflateMenu(menu)
        }
    }

    private fun scrollToPosition(position: Int) {
        binding.photoRecycler.scrollToCenter(position)
    }

    internal fun onSolSelected(toLong: Long) {
        homeViewModel.getLandingDateInMillis()?.let { landing_date_in_millis ->
            utilities.calculateDaysEarthDate(
                toLong,
                landing_date_in_millis
            ).let {
                homeViewModel.setViewStateCurrentDate(it.formatMillisToDate().formatDateToMillis())
                homeViewModel.getCurrentDate()?.let { currentDate ->
                    onDateSelected(currentDate, true)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {

        if (!viewModel.isSavedView) {
            binding.bottomAppbar.dateButtonText.setOnClickListener {
                binding.photoRecycler.stopScroll()
                homeViewModel.setViewStateShowDatePicket(true)
            }

            binding.bottomAppbar.solButtonText.setOnClickListener {
                homeViewModel.setViewStateShowSolSelected(true)
            }

            binding.photoRecycler.observeFirstItemPosition(firstItemPosition = { position ->
                adapter?.snapshot()?.let { items ->
                    if (items.isNotEmpty() && items.size > position)
                        items[position]?.let {
                            homeViewModel.setViewStateCurrentDate(it.earth_date)
                        }
                }
            })

            binding.photoRecycler.fastScrollListener(fastScrolled = {
                homeViewModel.setViewStateSetFastScrollerVisibility(true)
                homeViewModel.setViewStateSetFastScrollerDateVisibility(false)
            }, extraFastScrolled = {
//            showFastScrollerDate()
            })

            binding.photoRecycler.isIdle {
                if (it) {
                    homeViewModel.setViewStateSetFastScrollerVisibility(false)
                    homeViewModel.setViewStateSetFastScrollerDateVisibility(false)
                }
            }

            binding.solSlider.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    homeViewModel.setViewStateSetFastScrollerDateVisibility(true)
                } else if (event.action == MotionEvent.ACTION_UP) {
                    homeViewModel.setViewStateSetFastScrollerDateVisibility(false)
                }
                false
            }

            binding.solSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    homeViewModel.getLandingDateInMillis()?.let { landing_date_in_millis ->
                        val date =
                            (((progress.toLong()) * MILLIS_IN_A_SOL) + landing_date_in_millis)
                        homeViewModel.setViewStateScrollDateDisplayText(date.formatMillisToDisplayDate())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    homeViewModel.getLandingDateInMillis()?.let { landing_date_in_millis ->
                        val date =
                            ((binding.solSlider.progress.toLong() * MILLIS_IN_A_SOL) + landing_date_in_millis).formatMillisToDate()
                                .formatDateToMillis()
                        onDateSelected(date!!, true)
                    }
                }

            })

        }

        adapter?.addLoadStateListener { loadStates ->

            homeViewModel.setViewStateSetTopProgress(loadStates.source.prepend is LoadState.Loading)
            homeViewModel.setViewStateSetBottomProgress(loadStates.source.append is LoadState.Loading)
            homeViewModel.setViewStateSetMainProgress(loadStates.source.refresh is LoadState.Loading)

            if (homeViewModel.isNavigateToDate() &&
                loadStates.source.prepend !is LoadState.Loading &&
                loadStates.source.append !is LoadState.Loading &&
                loadStates.source.refresh !is LoadState.Loading
            ) {
                homeViewModel.setViewStateNavigateToDate(false)
                homeViewModel.getCurrentDate()?.let { currentDate ->
                    onDateSelected(currentDate)
                }
            }

            binding.emptyMessage.isVisible = loadStates.source.refresh is LoadState.Error

            adapter?.let { adapter ->
                if (loadStates.source.refresh is LoadState.NotLoading &&
                    loadStates.append.endOfPaginationReached &&
                    adapter.itemCount < 1
                ) {
                    binding.photoRecycler.isVisible = false
                    binding.emptyMessage.isVisible = true
                }
            }

        }

        binding.emptyMessage.setOnClickListener {
            homeViewModel.getData()
        }
    }

    private fun hideFastScrollerDate() {
        binding.scrollDateDisplayText.apply {
            if (alpha == 1f)
                animate()
                    .alpha(0f)
                    .setListener(null).duration = 400L
        }
    }

    private fun showFastScrollerDate() {
        hideFastScrollerJob?.cancel()
        binding.scrollDateDisplayText.apply {
            animate()
                .alpha(1.0f).duration = 200L

        }
    }

    private fun hideFastScroller() {
        binding.slideFrame.apply {
            if (alpha == 1f) {
                hideFastScrollerJob?.cancel()
                hideFastScrollerJob = lifecycleScope.launch {
                    delay(2000L)
                    animate()
                        .translationX(toDpi(48).toFloat())
                        .alpha(0.0f).duration = 400L
                }
            }
        }
    }

    private fun showFastScroller() {
        binding.slideFrame.apply {
            if (alpha == 0f) {
                animate()
                    .alpha(1.0f)
                    .translationX(0f)
                    .setListener(null).duration = 400L
            }
        }
    }

    internal fun onDateSelected(date: Long, fetch: Boolean = false) {
        adapter?.snapshot()?.let { itemSnapshotList ->
            homeViewModel.onDateSelected(date, fetch, itemSnapshotList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hideFastScrollerJob?.cancel()
    }

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int) {
        if (homeViewModel.isSelectedListEmpty()) {
            viewModel.setPosition(position)
            findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
        } else {
            homeViewModel.setSelection(marsRoverPhoto, position)
        }
    }

    override fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ): Boolean {
        homeViewModel.setSelection(marsRoverPhoto, position)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        saveJob?.cancel()
    }
}