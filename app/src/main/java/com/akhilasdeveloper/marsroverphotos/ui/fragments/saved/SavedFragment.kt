package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.SelectionChecker
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.bumptech.glide.RequestManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class SavedFragment : BaseFragment(R.layout.fragment_saved), RecyclerClickListener {


    private var _binding: FragmentSavedBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities
    private var master: RoverMaster? = null
    private var selectedList: ArrayList<MarsRoverPhotoTable> = arrayListOf()

    //    var selectedUriList: MutableMap<Long,Uri> = hashMapOf()
    var selectedPositions: ArrayList<Int> = arrayListOf()
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var downloadJob: Job? = null

    @Inject
    lateinit var requestManager: RequestManager
    private var adapter: MarsRoverSavedPhotoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        setBackPressCallBack()
        super.onCreate(savedInstanceState)
    }

    private fun setBackPressCallBack() {
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    if (selectedList.isNotEmpty()) {
                        uiCommunicationListener.showConsentSelectorDialog(getString(R.string.clear_selection),getString(
                                                    R.string.clear_selection_consent), onOkSelect = {
                            clearSelection()
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
        _binding = FragmentSavedBinding.bind(view)

        init()
        subscribeObservers()
    }

    private fun init() {
        setWindowInsets()
        adapter = MarsRoverSavedPhotoAdapter(this, requestManager, utilities = utilities)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter
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

        adapter?.addOnPagesUpdatedListener {
            if (adapter?.snapshot()?.isEmpty() == true)
                binding.emptyMessage.apply {
                    isVisible = true
                    text = "Empty\nTap to refresh"
                }
            else
                binding.emptyMessage.isVisible = false
        }

        binding.emptyMessage.setOnClickListener {
            getData()
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
        lifecycleScope.launch {
            if (utilities.isShowLikesConsent()) {
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
                        selectedList.forEach { currentData ->
                            currentData.let {
                                viewModel.updateLike(
                                    marsRoverPhotoTable = currentData
                                )
                            }
                        }
                        val list: ArrayList<MarsRoverPhotoTable> = arrayListOf()
                        list.addAll(selectedList)
                        clearSelection()
                        uiCommunicationListener.showSnackBarMessage(
                            "Items removed from Liked Photos",
                            "Undo",
                            onClick = {
                                list.forEach { currentData ->
                                    currentData.let {
                                        viewModel.updateLike(
                                            marsRoverPhotoTable = currentData
                                        )
                                    }
                                }
                                requireContext().showShortToast("Photos are added back")
                            })
                    }, onCancelSelect = {
                        if (it) {
                            lifecycleScope.launch {
                                utilities.setHideLikesConsent()
                            }
                        }
                    })
            } else {
                selectedList.forEach { currentData ->
                    currentData.let {
                        viewModel.updateLike(
                            marsRoverPhotoTable = currentData
                        )
                    }
                }
                val list: ArrayList<MarsRoverPhotoTable> = arrayListOf()
                list.addAll(selectedList)
                clearSelection()
                uiCommunicationListener.showSnackBarMessage(
                    "Items removed from Liked Photos",
                    "Undo",
                    onClick = {
                        list.forEach { currentData ->
                            currentData.let {
                                viewModel.updateLike(
                                    marsRoverPhotoTable = currentData
                                )
                            }
                        }
                        requireContext().showShortToast("Photos are added back")
                    })
            }
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

    private fun setTitle(name: String) {
        binding.topAppbar.homeToolbarTop.title = name
        binding.topAppbar.homeCollapsingToolbarTop.title = name
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

    private fun getDisplayName(rover: MarsRoverPhotoTable) =
        "${rover.rover_name}_${rover.camera_name}_${rover.earth_date.formatMillisToFileDate()}_${rover.photo_id}${Constants.CACHE_IMAGE_EXTENSION}"

    private fun hideSelectMenu() {
        if (binding.topAppbar.homeToolbarTop.menu.isNotEmpty()) {
            binding.topAppbar.homeToolbarTop.menu.clear()
            binding.topAppbar.homeToolbarTop.navigationIcon = null
            master?.let {
                setTitle(it.name + " Rover (Liked Photos)")
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

    private fun subscribeObservers() {
        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            val isHandled = it.hasBeenHandled()
            it.peekContent?.let { rover ->
                it.setAsHandled()
                master = rover
                setData()
                if (!isHandled) {
                    getData()
                }
            }
        })

        /*viewModel.dataStateLikedPhotos.observe(viewLifecycleOwner, {
            adapter?.submitData(viewLifecycleOwner.lifecycle, it)
        })*/

        viewModel.positionState.observe(viewLifecycleOwner, {
            scrollToPosition(it)
        })

        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
            it?.let {
                it.peekContent?.let { photos ->
                    adapter?.submitData(viewLifecycleOwner.lifecycle, photos)
                }
            }
        })
    }

    private fun scrollToPosition(position: Int) {
        binding.photoRecycler.scrollToCenter(position)
    }

    private fun getData() {
        master?.let { master ->
            viewModel.getLikedPhotos(master)
        }
    }

    private fun setData() {
        master?.let {
            setTitle(master!!.name + " Rover (Liked Photos)")
        }
    }

    override fun onItemSelected(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ) {
        if (selectedList.isEmpty()) {

            viewModel.setIsSavedView(true)
            findNavController().navigate(R.id.action_savedFragment_to_roverViewFragment)
            viewModel.setPosition(position)

            hideSelectMenu()
        } else {
            setSelection(marsRoverPhoto, position)
        }
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

    override fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ): Boolean {
        setSelection(marsRoverPhoto, position)
        return true
    }

    private fun setWindowInsets() {

        val recyclerBottomPadding = binding.photoRecycler.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { _, insets ->
            val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.photoRecycler.updatePadding(bottom = systemWindows.bottom + recyclerBottomPadding)
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.topAppbar.homeCollapsingToolbarTop) { _, insets ->
            val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topAppbar.homeToolbarTop.updateMarginAndHeight(top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

    }
}