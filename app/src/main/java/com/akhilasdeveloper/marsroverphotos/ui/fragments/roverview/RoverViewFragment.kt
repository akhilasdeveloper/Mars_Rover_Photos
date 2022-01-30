package com.akhilasdeveloper.marsroverphotos.ui.fragments.roverview

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.core.view.*
import androidx.viewpager2.widget.ViewPager2
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import android.graphics.Bitmap
import android.provider.MediaStore
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.akhilasdeveloper.marsroverphotos.utilities.*
import java.io.IOException
import android.content.Intent
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import android.app.WallpaperManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import javax.inject.Inject


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var requestManager: RequestManager
    private var adapter: MarsRoverPagerAdapter? = null
    private var isShow = true
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var currentData: MarsRoverPhotoTable? = null
    private var currentPosition: Int? = null

    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private var undoClickPositionData: MarsRoverPhotoTable? = null

    @Inject
    lateinit var utilities: Utilities

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoverviewBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
    }

    private fun setListeners() {
        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE)
                    enableMenu()
                else
                    disableMenu()
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setPosition(position)
            }

        }

        onPageChangeCallback?.let {
            binding.viewPage.registerOnPageChangeCallback(it)
        }

        binding.apply {
            like.setOnClickListener {
                if (viewModel.isSavedView)
                    setSavedLike()
                else
                    setLike()
            }

            setWallpaper.setOnClickListener {
                setWallpaper()
            }

            info.setOnClickListener {
                currentData?.let {
                    uiCommunicationListener.showInfoDialog(it)
                }
            }
            share.setOnClickListener {
                uiCommunicationListener.showMoreSelectorDialog(onImageSelect = {
                    shareAsImage()
                }, onLinkSelect = {
                    setShare()
                }, onDownloadSelect = {
                    updateOrRequestPermission()
                }, items = currentData?.let {
                    listOf(it)
                })
            }
        }

        adapter?.addOnPagesUpdatedListener {
            if (viewModel.isSavedView) {
                undoClickPositionData?.let { photo ->
                    Timber.d("undoClickPositionData subscribeObservers $photo")
                    adapter?.snapshot()?.indexOf(photo)?.let { pos ->
                        Timber.d("undoClickPositionData subscribeObservers pos $pos")
                        undoClickPositionData = null
                        viewModel.setPosition(pos)
                    }
                }
            }
        }
    }

    private fun setCurrentData() {
        currentPosition?.let {
            adapter?.snapshot()?.let { adapter ->
                if (adapter.size > it && it >= 0) {
                    currentData = adapter[it]
                    currentData?.let {
                        uiCommunicationListener.setInfoDetails(it)
                    }
                    getIsLiked()
                }
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

    private fun setWallpaper() {
        currentData?.img_src?.downloadImageAsUri(requestManager) { uri ->
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

    private fun setShare() {
        currentData?.let {
            val intent = Intent(Intent.ACTION_SEND)
            val shareBody = resources.getString(
                R.string.share_text,
                currentData?.rover_name,
                currentData?.img_src
            )
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(intent, shareBody))
        }
    }

    private fun shareAsImage() {
        currentData?.img_src?.downloadImageAsBitmap(requireContext()) { bmp ->
            bmp?.let {
                lifecycleScope.launch {
                    val uriFile = withContext(Dispatchers.IO) {
                        utilities.toImageURI(
                            bitmap = bmp,
                            displayName = getDisplayName()
                        )
                    }
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_STREAM, uriFile)
                    intent.type = "image/png"
                    startActivity(Intent.createChooser(intent, "Share Via"))
                }
            }
        }
    }

    private fun setDownload() {
        if (writePermissionGranted) {
            currentData?.img_src?.downloadImageAsBitmap(requireContext()) { image ->
                image?.let {
                    savePhotoToExternalStorage(getDisplayName(), it).let { uri ->
                        if (uri != null) {
                            uiCommunicationListener.showSnackBarMessage(
                                "Image Saved to Gallery",
                                "View Image"
                            ) {
                                val intent = Intent()
                                intent.action = Intent.ACTION_VIEW
                                intent.setDataAndType(
                                    uri,
                                    "image/*"
                                )
                                startActivity(intent)
                            }
                        } else {
                            uiCommunicationListener.showSnackBarMessage("Failed to Save Image")
                        }
                    }
                }
            }
        }
    }

    private fun getDisplayName() =
        "${currentData!!.rover_name}_${currentData!!.camera_name}_${currentData!!.earth_date.formatMillisToFileDate()}_${currentData!!.photo_id}$${Constants.CACHE_IMAGE_EXTENSION}"

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Uri? {
        val imageCollection = sdk29andUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }

        try {
            requireActivity().contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream))
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

    private fun getIsLiked() {
        currentData?.let {
            it.photo_id.let { id ->
                viewModel.isLiked(id)
            }
        }
    }

    private fun setLike() {
        currentData?.let { photo ->
            viewModel.updateLike(
                marsRoverPhotoTable = photo
            )
        }
    }

    private fun setSavedLike() {
        lifecycleScope.launch {
            if (utilities.isShowLikeConsent()) {
                val pos = currentData?.copy()
                Timber.d("undoClickPositionData setSavedLike pos $currentPosition")
                uiCommunicationListener.showConsentSelectorDialog(getString(R.string.remove_from_liked_photos),
                    getString(
                        R.string.selected_items_will_be_removed
                    ), doNotShow = true,
                    onOkSelect = {
                        if (it) {
                            lifecycleScope.launch {
                                utilities.setHideLikeConsent()
                            }
                        }
                        currentData?.let { currentData ->
                            viewModel.updateLike(
                                marsRoverPhotoTable = currentData
                            )
                        }

                        val data = currentData
                        uiCommunicationListener.showSnackBarMessage(
                            "Item removed from Liked Photos",
                            "Undo",
                            onClick = {
                                pos?.let {
                                    Timber.d("undoClickPositionData setSavedLike $pos")
                                    undoClickPositionData = pos
                                    data?.let { currentData ->
                                        currentData.let {
                                            viewModel.updateLike(
                                                marsRoverPhotoTable = currentData
                                            )
                                        }
                                    }
                                    requireContext().showShortToast("Photo added back")
                                }
                            })
                    }, onCancelSelect = {
                        if (it) {
                            lifecycleScope.launch {
                                utilities.setHideLikesConsent()
                            }
                        }
                    })
            } else {
                val pos = currentData?.copy()
                Timber.d("undoClickPositionData setSavedLike pos $currentPosition")

                currentData?.let { currentData ->
                    viewModel.updateLike(
                        marsRoverPhotoTable = currentData
                    )
                }

                val data = currentData
                uiCommunicationListener.showSnackBarMessage(
                    "Item removed from Liked Photos",
                    "Undo",
                    onClick = {
                        pos?.let {
                            Timber.d("undoClickPositionData setSavedLike $pos")
                            undoClickPositionData = pos
                            data?.let { currentData ->
                                currentData.let {
                                    viewModel.updateLike(
                                        marsRoverPhotoTable = currentData
                                    )
                                }
                            }
                            requireContext().showShortToast("Photo added back")
                        }
                    })

            }
        }

    }

    private fun subscribeObservers() {
        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("dataStatePaging : $it")
                it.peekContent?.let { photos ->
                    adapter?.submitData(viewLifecycleOwner.lifecycle, photos)
                    setCurrentData()
                }
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner, {
            it.contentIfNotHandled?.let { position->
                currentPosition = position
                setCurrentData()
                if (position != binding.viewPage.currentItem)
                    binding.viewPage.setCurrentItem(position, false)
            }
        })
        viewModel.dataStateIsLiked.observe(viewLifecycleOwner, {
            updateLikeIcon(it)
        })
    }

    private fun init() {

        var isSet = true
        adapter = MarsRoverPagerAdapter(this, requestManager)
        ViewCompat.setOnApplyWindowInsetsListener(binding.menus) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            if (isSet) {
                binding.menus.updatePadding(bottom = systemWindows.bottom)
                isSet = false
            }
            return@setOnApplyWindowInsetsListener insets
        }

        binding.viewPage.adapter = adapter

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                writePermissionGranted = it
                setDownload()
            }
        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.getBitmap(requireContext())?.let { bitmap ->
                    val wallpaperManager =
                        WallpaperManager.getInstance(requireActivity().applicationContext)
                    lifecycleScope.launch {
                        wallpaperManager.setBitmap(bitmap)
                        requireContext().showShortToast(message = "Wallpaper set")
                    }
                }
            }
        }
    }

    private fun updateLikeIcon(liked: Boolean) {
        if (liked) {
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_heart_fill, 0, 0)
        } else {
            binding.like.setCompoundDrawablesWithIntrinsicBounds(
                0,
                com.akhilasdeveloper.marsroverphotos.R.drawable.ic_heart_unfill,
                0,
                0
            )
        }
    }

    private fun disableMenu() {
        binding.setWallpaper.isEnabled = false
        binding.share.isEnabled = false
        binding.like.isEnabled = false
        binding.info.isEnabled = false
    }

    private fun enableMenu() {
        binding.setWallpaper.isEnabled = true
        binding.share.isEnabled = true
        binding.like.isEnabled = true
        binding.info.isEnabled = true
    }

    private fun peekUI() {
        if (isShow)
            hide()
        else
            show()
    }

    private fun hide() {
        if (isShow) {
            isShow = false
            uiCommunicationListener.hideSystemBar()
            binding.menus.visibility = View.INVISIBLE
            binding.topGradient.visibility = View.INVISIBLE
        }
    }

    private fun show() {
        if (!isShow) {
            isShow = true
            uiCommunicationListener.showSystemBar()
            binding.menus.visibility = View.VISIBLE
            binding.topGradient.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        show()
    }

    override fun onClick() {
        peekUI()
    }

}