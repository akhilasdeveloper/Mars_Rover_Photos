package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.transition.TransitionInflater
import androidx.viewpager2.widget.ViewPager2
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
    private lateinit var controler: WindowInsetsControllerCompat
    private var isShow = true
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var currentData: MarsRoverPhotoDb? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoverviewBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
    }

    private fun setListeners() {
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { _ ->
                updateLikeIcon()
            }
        }
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(viewLifecycleOwner,  { response ->
            response?.let {
                adapter.submitData(viewLifecycleOwner.lifecycle, response)
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner,  {
            binding.viewPage.setCurrentItem(it, false)
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
        controler = WindowInsetsControllerCompat(requireActivity().window, binding.container)

        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback(){

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE)
                    enableMenu()
                else
                    disableMenu()
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentData = adapter.snapshot()[position]

                updateLikeIcon()

                binding.like.setOnClickListener {
                    currentData?.let {
                        it.id?.let { id->
                            viewModel.updateLike(!it.liked, id)
                        }
                    }
                }

                val dirPath = requireContext().getExternalFilesDir(null)?.absolutePath + "/" + getString(R.string.app_name) + "/"

                val dir = File(dirPath)

                val fileName: String = System.currentTimeMillis().toString()
/*
                Glide.with(requireActivity())
                    .load(currentData?.img_src)
                    .into(object : CustomTarget<Drawable?>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in Drawable?>?
                        ) {
                            val bitmap = (resource as BitmapDrawable).bitmap

                            binding.download.setOnClickListener {
                                if (verifyPermissions()) {
                                    saveImage(bitmap, dir, fileName)
                                }
                            }
                        }

                        override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                        override fun onLoadFailed(@Nullable errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            Toast.makeText(
                                requireContext(),
                                "Failed to Download Image! Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })*/
//                Toast.makeText(requireContext(),"Value ",Toast.LENGTH_SHORT).show()
            }
        }
        onPageChangeCallback?.let {
            binding.viewPage.registerOnPageChangeCallback(it)
        }

        setTheme()
        show()
    }

    private fun updateLikeIcon() {
        if (currentData?.liked == true){
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.ic_heart_fill, 0, 0)
        }else{
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.ic_heart_unfill, 0, 0)
        }
    }

    private fun disableMenu() {
        binding.setWallpaper.isEnabled = false
        binding.setWallpaper.alpha = .5f
        binding.download.isEnabled = false
        binding.download.alpha = .5f
        binding.share.isEnabled = false
        binding.share.alpha = .5f
        binding.like.isEnabled = false
        binding.like.alpha = .5f
    }

    private fun enableMenu() {
        binding.setWallpaper.isEnabled = true
        binding.setWallpaper.alpha = 1f
        binding.download.isEnabled = true
        binding.download.alpha = 1f
        binding.share.isEnabled = true
        binding.share.alpha = 1f
        binding.like.isEnabled = true
        binding.like.alpha = 1f
    }

    private fun saveImage(bitmap: Bitmap?, storageDir: File, imageFileName: String) {
        var successDirCreated = false
        if (!storageDir.exists()) {
            successDirCreated = storageDir.mkdir()
        }
        if (successDirCreated) {
            val imageFile = File(storageDir, imageFileName)
            val savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
                Toast.makeText(requireContext(), "Image Saved! $savedImagePath", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error while saving image!", Toast.LENGTH_SHORT)
                    .show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to make folder!", Toast.LENGTH_SHORT).show()
        }
    }

    fun verifyPermissions(): Boolean {

        // This will return the current Status
        val permissionExternalMemory =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionExternalMemory != PackageManager.PERMISSION_GRANTED) {
            val storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // If permission not granted then ask for permission real time.
            ActivityCompat.requestPermissions(requireActivity(), storagePermission, 1)
            return false
        }
        return true
    }

    private fun setTheme() {
        requireActivity().window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
        controler.isAppearanceLightStatusBars = false
        controler.isAppearanceLightNavigationBars = false
    }

    private fun removeTheme() {
        requireActivity().window.apply {
            statusBarColor = ResourcesCompat.getColor(resources, R.color.system_border, null)
            navigationBarColor = ResourcesCompat.getColor(resources, R.color.system_border, null)
        }
        controler.isAppearanceLightStatusBars = !requireContext().isDarkThemeOn()
        controler.isAppearanceLightNavigationBars = !requireContext().isDarkThemeOn()
    }

    private fun peekUI() {
        if (isShow)
            hide()
        else
            show()
    }

    private fun hide() {
        isShow = false
        controler.hide(WindowInsetsCompat.Type.systemBars())
        controler.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        binding.menus.visibility = View.INVISIBLE
    }

    private fun show() {
        isShow = true
        controler.show(WindowInsetsCompat.Type.systemBars())
        binding.menus.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeTheme()
        show()
        _binding = null
        onPageChangeCallback = null
    }

    override fun onClick() {
        peekUI()
    }

    override fun loaded(binding: ViewPagerItemBinding, photo: MarsRoverPhotoDb, position: Int) {
        this.binding.setWallpaper.setOnClickListener {
        }
    }

    private fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

}