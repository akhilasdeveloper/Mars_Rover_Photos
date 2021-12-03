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
import com.akhilasdeveloper.marsroverphotos.Constants.DISABLED_MENU_ALPHA
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.showShortToast
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.app.DownloadManager
import android.net.Uri
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.getSystemService
import com.akhilasdeveloper.marsroverphotos.Utilities
import javax.inject.Inject
import androidx.annotation.NonNull
import com.bumptech.glide.request.transition.Transition


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
    private lateinit var controler: WindowInsetsControllerCompat
    private var isShow = true
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var currentData: MarsRoverPhotoDb? = null
    private var currentPosition: Int? = null
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
        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback(){

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE)
                    enableMenu()
                else
                    disableMenu()
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                setCurrentData()
                updateUI()
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

        adapter.addLoadStateListener {
            setCurrentData()
            updateUI()
        }
    }

    private fun setCurrentData() {
        currentPosition?.let {
            currentData = adapter.snapshot()[it]
        }
    }

    private fun setWallpaper() {

    }

    private fun setShare() {

    }

    private fun setDownload() {
        currentData?.img_src?.let { url->
            utilities.downloadImage(url){
                showShortToast(it.toString())
            }
        }
    }

    private fun setLike() {
        currentData?.let {
            it.id?.let { id->
                viewModel.updateLike(!it.liked, id)
            }
        }
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(viewLifecycleOwner,  { response ->
            response?.let {
                adapter.submitData(viewLifecycleOwner.lifecycle, response)
                setCurrentData()
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner,  {
            currentPosition = it
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

        setTheme()
        show()
    }

    private fun updateUI() {
        if (currentData?.liked == true){
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.ic_heart_fill, 0, 0)
        }else{
            binding.like.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.ic_heart_unfill, 0, 0)
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

    private fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

}