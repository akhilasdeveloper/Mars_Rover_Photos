package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.WindowManager
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
class RoverViewFragment: BaseFragment(R.layout.fragment_roverview),PagerClickListener {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter(this)
    private lateinit var controler: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
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
                adapter.submitData(viewLifecycleOwner.lifecycle,response)
            }
        })
        viewModel.positionState.observe(viewLifecycleOwner, Observer {
            binding.viewPage.setCurrentItem(it, false)
        })
    }

    private fun init() {
        binding.viewPage.adapter = adapter
        controler = WindowInsetsControllerCompat(requireActivity().window, binding.container)
        peekUI()
    }

    private fun peekUI(){
        CoroutineScope(Dispatchers.Main).launch {
            show()
            delay(2000)
            if (!isDetached)
                hide()
        }
    }

    private fun hide() {
        controler.hide(WindowInsetsCompat.Type.systemBars())
        controler.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun show() {
        controler.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        show()
    }

    override fun onClick() {
        peekUI()
    }

}