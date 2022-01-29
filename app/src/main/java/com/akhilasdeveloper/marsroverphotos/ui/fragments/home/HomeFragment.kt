package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.AD_ENABLED
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.CACHE_IMAGE_EXTENSION
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import com.bumptech.glide.RequestManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.google.android.gms.ads.AdRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

import com.akhilasdeveloper.marsroverphotos.databinding.*
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.SelectionChecker


@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var requestManager: RequestManager
    lateinit var homeViewModel: HomeViewModel
    private var adapter: MarsRoverPhotoAdapter? = null
    internal var master: RoverMaster? = null
    internal var currentDate: Long? = null
    private var hideFastScrollerJob: Job? = null
    private var downloadJob: Job? = null
    private var navigateToDate = false
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
                        uiCommunicationListener.showConsentSelectorDialog(getString(R.string.clear_selection),
                            getString(
                                R.string.clear_selection_consent
                            ),
                            onOkSelect = {
                                homeViewModel.clearSelection()
                            })
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
                binding.topAppbar.homeToolbarTop.title = it
                binding.topAppbar.homeCollapsingToolbarTop.title = it
            })
            viewStateSelectedTitle.observe(viewLifecycleOwner, {
                binding.homeBottomToolbarSecond.title = it
            })
            viewStateSolButtonText.observe(viewLifecycleOwner, { solButtonText ->
                binding.bottomAppbar.solButtonText.text = getString(
                    R.string.sol, solButtonText
                )
            })
            viewStateRoverMaster.observe(viewLifecycleOwner, { roverMaster ->
                master = roverMaster
            })
            viewStateCurrentDate.observe(viewLifecycleOwner, { currentDate ->
                this@HomeFragment.currentDate = currentDate
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

            dataStatePaging.observe(viewLifecycleOwner, {
                it?.let {
                    val isHandled = it.hasBeenHandled()
                    it.peekContent?.let { photos ->
                        it.setAsHandled()
                        adapter?.submitData(viewLifecycleOwner.lifecycle, photos)
                        if (!isHandled) {
                            onDateSelected(currentDate!!)
                        }
                    }
                }
            })

            dataStateSelectedList.observe(viewLifecycleOwner, {

            })
        }
    }

    private fun subscribeObservers() {

        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            homeViewModel.setViewStateRoverMaster(it)
        })

        viewModel.positionState.observe(viewLifecycleOwner, {
            scrollToPosition(it)
        })

    }

    private fun init() {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        setWindowInsets()
        adapter = MarsRoverPhotoAdapter(this, requestManager)

        val layoutManager = GridLayoutManager(
            requireContext(),
            GALLERY_SPAN,
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
                            GALLERY_SPAN
                    else
                        GALLERY_SPAN
                } ?: return GALLERY_SPAN
            }
        }

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter
        }
        homeViewModel.setViewStateSetFastScrollerVisibility(false)
        if (AD_ENABLED) {
            binding.adView.root.isVisible = true
            val adRequest: AdRequest = AdRequest.Builder().build()
            binding.adView.adView.loadAd(adRequest)
        }

        binding.homeBottomToolbarSecond.setNavigationOnClickListener {
            homeViewModel.clearSelection()
        }

        binding.homeBottomToolbarSecond.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.share -> {
                    uiCommunicationListener.showMoreSelectorDialog(
                        onImageSelect = {
                            shareAllAsImage()
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
                        })
                    true
                }
                R.id.favorites -> {
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
                        requireContext().showShortToast(message = "Wallpaper set")
                    }
                }
            }
        }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                writePermissionGranted = it
                setDownload()
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
                        ), buttonText = requireContext().getString(R.string.allow), onClick = {
                            updateOrRequestPermission()
                        })
                })
        } else {
            setDownload()
        }
    }

    private fun setDownload() {
        if (writePermissionGranted) {
            downloadJob?.cancel()
            downloadJob = CoroutineScope(Dispatchers.IO).launch {
                homeViewModel.getSelectedList().forEachIndexed { index, currentData ->
                    download(currentData, index)
                }
                withContext(Dispatchers.Main) {
                    uiCommunicationListener.showSnackBarMessage(
                        "Image(s) Saved to Gallery",
                    )
                    uiCommunicationListener.hideDownloadProgressDialog()
                }
            }
        }
    }


    private suspend fun download(currentData: MarsRoverPhotoTable, index: Int = 0): Uri? {
        var uri: Uri? = null
        withContext(Dispatchers.Main) {
            uiCommunicationListener.showDownloadProgressDialog(
                ((index.plus(1)
                    .toFloat() / homeViewModel.getSelectedList().size.toFloat()) * 100).toInt()
            ) {
                uiCommunicationListener.hideDownloadProgressDialog()
                downloadJob?.cancel()
            }
        }
        currentData.img_src.downloadImageAsBitmap(requireContext()) { image ->
            image?.let {
                uri = savePhotoToExternalStorage(getDisplayName(currentData), it)
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
                if (it.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                    intent.type = "image/png"
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, it)
                    startActivity(Intent.createChooser(intent, "Share Via"))
                }
                withContext(Dispatchers.Main) {
                    uiCommunicationListener.hideDownloadProgressDialog()
                }
            }
        }
    }

    private fun getDisplayName(rover: MarsRoverPhotoTable) =
        "${rover.rover_name}_${rover.camera_name}_${rover.earth_date.formatMillisToFileDate()}_${rover.photo_id}$CACHE_IMAGE_EXTENSION"

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
        utilities.calculateDaysEarthDate(
            toLong,
            master!!.landing_date_in_millis
        ).let {
            currentDate = it.formatMillisToDate().formatDateToMillis()
            onDateSelected(currentDate!!, true)
        }
    }

    private fun getData() {
        currentDate?.let { currentDate ->
            master?.let { master ->
                homeViewModel.getData(master, currentDate)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        binding.bottomAppbar.dateButtonText.setOnClickListener {
            binding.photoRecycler.stopScroll()
            showDatePicker()
        }

        binding.bottomAppbar.solButtonText.setOnClickListener {
            showSolSelectorDialog()
        }
        adapter?.addLoadStateListener { loadStates ->

            homeViewModel.setViewStateSetTopProgress(loadStates.source.prepend is LoadState.Loading)
            homeViewModel.setViewStateSetBottomProgress(loadStates.source.append is LoadState.Loading)
            homeViewModel.setViewStateSetMainProgress(loadStates.source.refresh is LoadState.Loading)

            if (navigateToDate &&
                loadStates.source.prepend !is LoadState.Loading &&
                loadStates.source.append !is LoadState.Loading &&
                loadStates.source.refresh !is LoadState.Loading
            ) {
                navigateToDate = false
                onDateSelected(currentDate!!)
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
            getData()
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

        binding.solSlider.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                homeViewModel.setViewStateSetFastScrollerDateVisibility(true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                homeViewModel.setViewStateSetFastScrollerDateVisibility(false)
            }
            false
        }

        binding.solSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                master?.let { rover ->
                    val date =
                        ((progress.toLong() * MILLIS_IN_A_DAY) + rover.landing_date_in_millis)
                    homeViewModel.setViewStateScrollDateDisplayText(date.formatMillisToDisplayDate())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                master?.let { rover ->
                    val date =
                        ((binding.solSlider.progress.toLong() * MILLIS_IN_A_DAY) + rover.landing_date_in_millis).formatMillisToDate()
                            .formatDateToMillis()
                    onDateSelected(date!!, true)
                }
            }


        })


        val params = binding.bottomAppbar.homeAppbar.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = object : HideListenableBottomAppBarBehavior() {
            override fun onSlideDown() {
                showAd()
            }

            override fun onSlideUp() {
                hideAd()
            }
        }
    }

    private fun showAd() {
        binding.adView.itemAdBanner.apply {
            if (AD_ENABLED) {
                animate()
                    .alpha(1.0f)
                    .setListener(null).duration = 800L
            }
        }
    }

    private fun hideAd() {
        binding.adView.itemAdBanner.apply {
            if (AD_ENABLED) {
                animate()
                    .alpha(0.0f)
                    .setListener(null).duration = 800L
            }
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
        currentDate = date
        homeViewModel.setViewStateCurrentDate(date)
        val snapShot = adapter?.snapshot()
        val search = snapShot?.filter { photo ->
            photo?.earth_date == date && photo.is_placeholder
        }
        if (search?.isNotEmpty() == true) {
            val pos = snapShot.indexOf(search[0])
            scrollToPosition(pos)
        } else if (fetch) {
            navigateToDate = true
            getData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hideFastScrollerJob?.cancel()
    }

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int) {
        if (homeViewModel.isSelectedListEmpty()) {
            findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
            viewModel.setPosition(position)
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
            val displayName = getDisplayName(photo)

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


}