package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.akhilasdeveloper.marsroverphotos.ui.MainViewModel
import com.akhilasdeveloper.marsroverphotos.ui.UICommunicationListener
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.bumptech.glide.RequestManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseFragment(layout: Int): Fragment(layout) {

    lateinit var viewModel: MainViewModel
    lateinit var uiCommunicationListener: UICommunicationListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try{
            uiCommunicationListener = context as UICommunicationListener
        }catch(e: ClassCastException){
            Timber.e( "$context must implement UICommunicationListener" )
        }
    }
}