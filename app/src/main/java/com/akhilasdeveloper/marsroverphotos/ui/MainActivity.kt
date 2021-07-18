package com.akhilasdeveloper.marsroverphotos.ui

import android.os.Bundle
import androidx.core.view.WindowCompat
import com.akhilasdeveloper.marsroverphotos.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun setupActionBar(id: MaterialToolbar) {
        setSupportActionBar(id)
    }
}