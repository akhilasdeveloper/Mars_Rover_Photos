package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.akhilasdeveloper.marsroverphotos.utilities.*

import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.RecyclerClickListener

@AndroidEntryPoint
class SavedFragment : BaseFragment(R.layout.fragment_saved), RecyclerClickListener {

    private var _binding: FragmentSavedBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

    private val adapter = MarsRoverPhotoAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSavedBinding.bind(view)

        init()
        subscribeObservers()
    }

    private fun init() {

    }

    private fun subscribeObservers() {

    }

    override fun onItemSelected(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ) {

    }

}