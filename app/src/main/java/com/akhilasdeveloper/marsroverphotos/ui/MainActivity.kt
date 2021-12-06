package com.akhilasdeveloper.marsroverphotos.ui

import android.os.Bundle
import androidx.core.view.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.ActivityMainBinding
import com.akhilasdeveloper.marsroverphotos.utilities.isDarkThemeOn
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(R.color.black)

        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarBg) { view, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams { height = systemWindows.top }
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationBarBg) { view, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams { height = systemWindows.bottom }
            return@setOnApplyWindowInsetsListener insets
        }

        destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.roverViewFragment -> {
                        setTransparentSystemBar()
                        setStatusBarDarkTheme()
                    }
                    else -> {
                        removeTransparentSystemBar()
                        setStatusBarContrast()
                    }
                }
            }

        navController = findNavController(R.id.navHostFragment)

        destinationChangedListener?.let {
            navController?.addOnDestinationChangedListener(it)
        }

    }

    override fun hideSystemBar() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
    }

    override fun showSystemBar() {
        WindowInsetsControllerCompat(
            window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setTransparentSystemBar() {
        if (binding.navigationBarBg.alpha == 1f) {
            binding.navigationBarBg.animate().alpha(0f).duration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            binding.statusBarBg.animate().alpha(0f).duration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        }
    }

    private fun removeTransparentSystemBar() {
        if (binding.navigationBarBg.alpha == 0f) {
            binding.navigationBarBg.animate().alpha(1f).duration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            binding.statusBarBg.animate().alpha(1f).duration =
                resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        }
    }

    private fun setStatusBarDarkTheme() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun setStatusBarContrast() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !applicationContext.isDarkThemeOn()
            isAppearanceLightNavigationBars = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destinationChangedListener?.let {
            navController?.removeOnDestinationChangedListener(it)
            destinationChangedListener = null
        }
    }

}