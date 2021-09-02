package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Observer
import androidx.transition.TransitionInflater
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

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
        setListeners()
        subscribeObservers()
        getData()
    }

    private fun setListeners() {
        binding.viewPage.setOnClickListener {
            peekUI()
        }
    }

    private fun getData() {
//        viewModel.getData(date = "2021-07-20", api_key = Constants.API_KEY)
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
        binding.viewPage.adapter = adapter
        controler = WindowInsetsControllerCompat(requireActivity().window, binding.container)
        setTheme()
        show()
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
        controler.hide(WindowInsetsCompat.Type.systemBars())
        controler.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun show() {
        controler.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeTheme()
        show()
    }

    override fun onClick() {
        peekUI()
    }

    private fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

}