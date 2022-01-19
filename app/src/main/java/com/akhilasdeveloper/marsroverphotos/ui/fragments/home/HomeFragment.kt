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
import com.google.android.material.appbar.AppBarLayout

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

    private var adapter: MarsRoverPhotoAdapter? = null
    internal var master: RoverMaster? = null
    internal var currentDate: Long? = null
    private var hideFastScrollerJob: Job? = null
    private var downloadJob: Job? = null
    private var navigateToDate = false
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedList: ArrayList<MarsRoverPhotoTable> = arrayListOf()

    //    var selectedUriList: MutableMap<Long,Uri> = hashMapOf()
    var selectedPositions: ArrayList<Int> = arrayListOf()

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

                    if (selectedList.isNotEmpty()) {
                        clearSelection()
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
    }

    private fun init() {

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
        hideFastScroller()
        if (AD_ENABLED) {
            binding.adView.root.isVisible = true
            val adRequest: AdRequest = AdRequest.Builder().build()
            binding.adView.adView.loadAd(adRequest)
        }

        binding.topAppbar.homeToolbarTop.setNavigationOnClickListener {
            clearSelection()
        }

        binding.topAppbar.homeToolbarTop.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.share -> {
                    uiCommunicationListener.showMoreSelectorDialog(onImageSelect = {
                        shareAllAsImage()
                    }, onLinkSelect = {
                        shareAllAsLinks()
                    }, onDownloadSelect = {
                        updateOrRequestPermission()
                    })
                    true
                }
                R.id.favorites -> {
                    setLike()
                    true
                }
                R.id.wallpaper -> {
                    if (selectedList.isNotEmpty())
                        setWallpaper(selectedList[0])
                    true
                }
                else -> false
            }
        }

        adapter?.selectionChecker = object : SelectionChecker {
            override fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean =
                if (selectedList.isNotEmpty()) selectedList.contains(marsRoverPhotoTable) else false
        }

        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.getBitmap(requireContext())?.let { bitmap ->
                    val wallpaperManager =
                        WallpaperManager.getInstance(requireActivity().applicationContext)
                    lifecycleScope.launch {
                        wallpaperManager.setBitmap(bitmap)
                        clearSelection()
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

    private fun pinToolbar() {
        (binding.topAppbar.homeCollapsingToolbarTop.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
            (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED)
    }

    private fun unPinToolbar() {
        (binding.topAppbar.homeCollapsingToolbarTop.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
            (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED)
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
                selectedList.forEachIndexed { index, currentData ->
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
                ((index.plus(1).toFloat() / selectedList.size.toFloat()) * 100).toInt()
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

    private fun clearSelection() {
        hideSelectMenu()
        selectedList.clear()
        selectedPositions.forEach {
            adapter?.notifyItemChanged(it)
        }
        selectedPositions.clear()
    }

    private fun setLike() {
        selectedList.forEach { currentData ->
            currentData.let {
                viewModel.updateLike(
                    marsRoverPhotoTable = currentData
                )
            }
        }
        clearSelection()
        requireContext().showShortToast("Added to liked photos")
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
        if (selectedList.isNotEmpty()) {
            var links = ""
            selectedList.forEach {
                links += "${it.img_src} \n"
            }
            selectedList[0].let {
                val intent = Intent(Intent.ACTION_SEND)
                val shareBody = resources.getString(
                    if (selectedList.size == 1) R.string.share_text else R.string.share_texts,
                    it.rover_name,
                    links
                )
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, shareBody)
                startActivity(Intent.createChooser(intent, getString(R.string.share_text)))
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

    private fun hideSelectMenu() {
        if (binding.topAppbar.homeToolbarTop.menu.isNotEmpty()) {
            binding.topAppbar.homeToolbarTop.menu.clear()
            binding.topAppbar.homeToolbarTop.navigationIcon = null
            master?.let {
                setTitle(it.name)
            }
            unPinToolbar()
        }
    }

    private fun showSelectMenu() {
        val menu =
            if (selectedList.size == 1) R.menu.top_appbar_single_item_select_menu else R.menu.top_appbar_select_menu
        val menuSize = if (selectedList.size == 1) 3 else 2
        if (binding.topAppbar.homeToolbarTop.menu.isEmpty() || binding.topAppbar.homeToolbarTop.menu.size() != menuSize) {
            if (binding.topAppbar.homeToolbarTop.menu.isNotEmpty())
                binding.topAppbar.homeToolbarTop.menu.clear()
            binding.topAppbar.homeToolbarTop.inflateMenu(menu)
            binding.topAppbar.homeToolbarTop.navigationIcon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_x, null)
            binding.topAppbar.homeToolbarTop.setNavigationIconTint(
                ResourcesCompat.getColor(
                    resources,
                    R.color.system_for,
                    null
                )
            )
        }
        setTitle("Selected (${selectedList.size})")
        pinToolbar()
    }

    private fun showMainProgress() {
        uiCommunicationListener.showIndeterminateProgressDialog()
    }

    private fun hideMainProgress() {
        uiCommunicationListener.hideIndeterminateProgressDialog()
    }

    private fun subscribeObservers() {

        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            val isHandled = it.hasBeenHandled()
            it.peekContent?.let { rover ->
                it.setAsHandled()
                master = rover

                setData()
                if (!isHandled) {
                    currentDate = rover.max_date_in_millis
                    viewModel.setDate(currentDate!!)
                    getData()
                }
            }
        })

        viewModel.dataStateDate.observe(viewLifecycleOwner, {
            currentDate = it
            setDateAndSolButtonText()
        })

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            if (it) showMainProgress()
            else
                hideMainProgress()
        })

        viewModel.positionState.observe(viewLifecycleOwner, {
            scrollToPosition(it)
        })

        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
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

    }

    private fun setData() {
        master?.let {
            setTitle(it.name)
            initFastScroller()
        }
    }

    private fun setTitle(name: String) {
        binding.topAppbar.homeToolbarTop.title = name
        binding.topAppbar.homeCollapsingToolbarTop.title = name
    }

    private fun initFastScroller() {
        binding.solSlider.apply {
            val count =
                (master!!.max_date_in_millis - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY
            this.max = count.toInt()
            this.progress = count.toInt()
        }
    }

    private fun setDateAndSolButtonText() {
        setDateButtonText()
        setSolButtonText()
        setFastScrollerValue()
    }

    private fun setFastScrollerValue() {
        master?.let {
            binding.solSlider.apply {
                val count = (currentDate!! - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY
                this.progress = count.toInt()
                hideFastScrollerDate()
            }
        }
    }

    private fun setSolButtonText() {
        master?.let {
            binding.bottomAppbar.solButtonText.text = getString(
                R.string.sol,
                utilities.calculateDays(it.landing_date_in_millis, currentDate).toString()
            )
        }
    }


    private fun setDateButtonText() {
        binding.bottomAppbar.dateButtonText.text = currentDate!!.formatMillisToDate()
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
                viewModel.getData(master, currentDate)
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

            binding.progressTop.isVisible = loadStates.source.prepend is LoadState.Loading
            binding.progress.isVisible = loadStates.source.append is LoadState.Loading
            viewModel.setLoading(loadStates.source.refresh is LoadState.Loading)

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
                        viewModel.setDate(it.earth_date)
                    }
            }
        })

        binding.photoRecycler.fastScrollListener(fastScrolled = {
            showFastScroller()
            hideFastScrollerDate()
        }, extraFastScrolled = {
//            showFastScrollerDate()
        })

        binding.photoRecycler.isIdle {
            if (it) {
                hideFastScroller()
                hideFastScrollerDate()
            }
        }

        binding.solSlider.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showFastScrollerDate()
            } else if (event.action == MotionEvent.ACTION_UP) {
                hideFastScrollerDate()
            }
            false
        }

        binding.solSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                master?.let { rover ->
                    val date =
                        ((progress.toLong() * MILLIS_IN_A_DAY) + rover.landing_date_in_millis)
                    binding.scrollDateDisplayText.text = date.formatMillisToDisplayDate()
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
        viewModel.setDate(date)
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
        if (selectedList.isEmpty()) {

            findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
            viewModel.setPosition(position)

            hideSelectMenu()
        } else {
            setSelection(marsRoverPhoto, position)
        }
    }

    override fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int,
        view: View,
        x: Float,
        y: Float
    ): Boolean {
        setSelection(marsRoverPhoto, position)
        return true
    }


    private fun setSelection(photo: MarsRoverPhotoTable, position: Int) {
        if (selectedList.contains(photo)) {
            selectedList.remove(photo)
            selectedPositions.remove(position)
        } else {
            selectedList.add(photo)
            selectedPositions.add(position)
        }
        if (selectedList.isEmpty())
            hideSelectMenu()
        else
            showSelectMenu()
        adapter?.notifyItemChanged(position)
    }

    private suspend fun downloadImage(selectedList: ArrayList<MarsRoverPhotoTable> = this.selectedList): ArrayList<Uri> {
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