package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.lifecycle.Observer
import androidx.transition.TransitionInflater
import androidx.viewpager2.widget.ViewPager2
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import com.github.piasy.biv.view.BigImageView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import com.bumptech.glide.Glide

import android.graphics.Bitmap

import android.graphics.drawable.Drawable

import android.graphics.drawable.BitmapDrawable

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.transition.Transition

import com.bumptech.glide.request.target.CustomTarget

import android.os.Environment
import com.akhilasdeveloper.marsroverphotos.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
    private lateinit var controler: WindowInsetsControllerCompat
    private var isShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoverviewBinding.bind(view)

        init()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(viewLifecycleOwner, Observer { response ->
            response?.let {
                adapter.submitData(viewLifecycleOwner.lifecycle, response)
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner, Observer {
            binding.viewPage.setCurrentItem(it, false)
        })
    }

    private fun init() {
        var isSet = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.menus) { v, insets ->
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

        binding.viewPage.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val datas = adapter.snapshot()[position]
                val dirPath =
                    requireContext().getExternalFilesDir(null)?.absolutePath + "/" + getString(R.string.app_name) + "/"

                val dir = File(dirPath)

                val fileName: String = System.currentTimeMillis().toString()

                Glide.with(requireActivity())
                    .load(datas?.img_src)
                    .into(object : CustomTarget<Drawable?>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in Drawable?>?
                        ) {
                            val bitmap = (resource as BitmapDrawable).bitmap

                            binding.download.setOnClickListener {

                                if (verifyPermissions()) {
                                    saveImage(bitmap, dir, fileName);
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
                    })
//                Toast.makeText(requireContext(),"Value ",Toast.LENGTH_SHORT).show()
            }
        })

        setTheme()
        show()
    }

    private fun saveImage(bitmap: Bitmap?, storageDir: File, imageFileName: String) {
        var successDirCreated = false
        if (!storageDir.exists()) {
            successDirCreated = storageDir.mkdir()
        }
        if (successDirCreated) {
            val imageFile: File = File(storageDir, imageFileName)
            val savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
                Toast.makeText(requireContext(), "Image Saved! " + savedImagePath.toString(), Toast.LENGTH_SHORT).show()
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
            val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // If permission not granted then ask for permission real time.
            ActivityCompat.requestPermissions(requireActivity(), STORAGE_PERMISSIONS, 1)
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
    }

    override fun onClick() {
        peekUI()
    }

    override fun loaded(bindingViewPager: ViewPagerItemBinding, photo: MarsRoverPhotoDb, position: Int) {
        binding.setWallpaper.setOnClickListener {
        }
    }

    private fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

}