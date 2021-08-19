package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoversBinding
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverAdapter
import timber.log.Timber

class RoversFragment : BaseFragment(R.layout.fragment_rovers){

    private var _binding: FragmentRoversBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MarsRoverAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoversBinding.bind(view)

        init()
//        setListeners()
        subscribeObservers()
        getData()
    }

    private fun init() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.recycler.updatePadding(bottom = systemWindows.bottom, top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        adapter= MarsRoverAdapter(requireContext())
        val layoutManager = LinearLayoutManager(requireContext())

        binding.apply {
            recycler.setHasFixedSize(true)
            recycler.layoutManager = layoutManager
            recycler.adapter = adapter
        }
    }

    private fun subscribeObservers() {
        viewModel.dataStateRover.observe(viewLifecycleOwner, Observer { response ->
            Timber.d("***Triggered ${response}")
            response.data?.let {
                adapter.submitList(it)
            }
            response.isLoading.let {
                binding.progress.isVisible = !(it == null || !it)
            }
            response.error?.let {
                Toast.makeText(requireContext(),it,Toast.LENGTH_LONG).show()
            }
        })

    }

    private fun getData() {
        if (viewModel.dataStateRover.value==null) {
            binding.progress.isVisible = true
            viewModel.getRoverData()
        }
    }

}