package com.akhilasdeveloper.marsroverphotos.ui

import androidx.appcompat.app.AppCompatActivity
import com.akhilasdeveloper.marsroverphotos.UICommunicationListener
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity: AppCompatActivity(), UICommunicationListener
{

}