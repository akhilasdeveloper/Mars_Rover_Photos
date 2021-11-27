package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.akhilasdeveloper.marsroverphotos.UICommunicationListener
import com.akhilasdeveloper.marsroverphotos.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
abstract class BaseFragment(layout: Int): Fragment(layout) {

    lateinit var uiCommunicationListener: UICommunicationListener
    lateinit var viewModel: MainViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try{
            uiCommunicationListener = context as UICommunicationListener
        }catch(e: ClassCastException){
            Timber.e( "$context must implement UICommunicationListener" )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }
}