package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.transition.TransitionInflater
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoverviewBinding
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPagerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoverViewFragment: BaseFragment(R.layout.fragment_roverview) {

    private var _binding: FragmentRoverviewBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPagerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoverviewBinding.bind(view)

        init()
//        setListeners()
        subscribeObservers()
        getData()
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
    }

}