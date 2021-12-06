package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import javax.inject.Inject
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoLikedDb


@AndroidEntryPoint
class RoverViewFragment : BaseFragment(R.layout.fragment_roverview), PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
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
            getIsLiked()
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

    private fun getIsLiked(){
        currentData?.let {
            it.id?.let { id ->
                viewModel.isLiked(id)
            }
        }
    }

    private fun setLike() {
        currentData?.let {
            it.id?.let { id->
                viewModel.updateLike(marsRoverPhotoLikedDb = MarsRoverPhotoLikedDb(
                    id = id,
                    rover_id = it.rover_id
                ))
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
            setCurrentData()
            if (it!=binding.viewPage.currentItem)
                binding.viewPage.setCurrentItem(it, false)
        })
        viewModel.dataStateIsLiked.observe(viewLifecycleOwner,{
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

    private fun updateLikeIcon(liked:Boolean) {
        if (liked){
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