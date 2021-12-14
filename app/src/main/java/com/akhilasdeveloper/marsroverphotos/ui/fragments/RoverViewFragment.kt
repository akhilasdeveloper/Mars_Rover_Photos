package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.*
import androidx.paging.PagingData
import androidx.viewpager2.widget.ViewPager2
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import com.akhilasdeveloper.marsroverphotos.ui.DepthPageTransformer
import com.akhilasdeveloper.marsroverphotos.ui.ZoomOutPageTransformer
import com.akhilasdeveloper.marsroverphotos.utilities.downloadImageAsBitmap
import timber.log.Timber
import android.graphics.Bitmap

import android.provider.MediaStore

import android.os.Environment

import android.content.ContentValues

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.akhilasdeveloper.marsroverphotos.utilities.downloadImageAsUri
import java.io.File
import java.lang.Exception


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
    private var isShow = true
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var currentData: MarsRoverPhotoTable? = null
    private var currentPosition: Int? = null

    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

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
                setLike()
            }
            download.setOnClickListener {
                setDownload()
            }
            share.setOnClickListener {
                setShare()
            }
            setWallpaper.setOnClickListener {
                setWallpaper()
            }
        }

    }

    private fun setCurrentData() {
        currentPosition?.let {
            currentData = adapter.snapshot()[it]
            Timber.d("position : $it : ${currentData?.photo_id}")
            getIsLiked()
        }
    }

    private fun updateOrRequestPermission(){
        val hasWritePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        writePermissionGranted = hasWritePermission || minSdk29

        if (!writePermissionGranted){
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun setWallpaper() {

    }

    private fun setShare() {

    }

    private fun setDownload() {
        currentData?.img_src?.downloadImageAsUri(requireContext()) {image->
            
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
        currentData?.let {
            it.photo_id.let { id ->
                viewModel.updateLike(
                    marsRoverPhotoLikedTable = MarsRoverPhotoLikedTable(
                        id = id,
                        rover_id = it.rover_id
                    )
                )
            }
        }
    }

    private fun subscribeObservers() {
        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("dataStatePaging : $it")
                it.peekContent?.let {photos->
                    adapter.submitData(viewLifecycleOwner.lifecycle, photos)
                    setCurrentData()
                }
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner, {
            currentPosition = it
            setCurrentData()
            if (it != binding.viewPage.currentItem)
                binding.viewPage.setCurrentItem(it, false)
        })
        viewModel.dataStateIsLiked.observe(viewLifecycleOwner, {
            updateLikeIcon(it)
        })
    }

    private fun init() {

        var isSet = true

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
    }

    private fun updateLikeIcon(liked: Boolean) {
        if (liked) {
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_heart_fill, 0, 0)
        } else {
            binding.like.setCompoundDrawablesWithIntrinsicBounds(
                0,
                R.drawable.ic_heart_unfill,
                0,
                0
            )
        }
    }

    private fun disableMenu() {
        binding.setWallpaper.isEnabled = false
        binding.download.isEnabled = false
        binding.share.isEnabled = false
        binding.like.isEnabled = false
    }

    private fun enableMenu() {
        binding.setWallpaper.isEnabled = true
        binding.download.isEnabled = true
        binding.share.isEnabled = true
        binding.like.isEnabled = true
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