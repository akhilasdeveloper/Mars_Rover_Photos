package com.akhilasdeveloper.marsroverphotos.ui.saved

import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.layout_sol_select.view.*
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import kotlinx.coroutines.*

import java.util.*
import kotlin.collections.ArrayList
import android.annotation.SuppressLint
import androidx.coordinatorlayout.widget.CoordinatorLayout

import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.AD_ENABLED
import com.google.android.gms.ads.AdRequest

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

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int) {

    }

}