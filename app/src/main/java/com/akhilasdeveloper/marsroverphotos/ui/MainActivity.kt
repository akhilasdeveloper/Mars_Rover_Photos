package com.akhilasdeveloper.marsroverphotos.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.*
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.ActivityMainBinding
import com.akhilasdeveloper.marsroverphotos.databinding.SnackBarLayoutBinding
import com.akhilasdeveloper.marsroverphotos.utilities.isDarkThemeOn
import com.akhilasdeveloper.marsroverphotos.utilities.sdkAndUp
import com.google.android.material.snackbar.Snackbar
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

        sdkAndUp(Build.VERSION_CODES.R, onSdkAndAbove = {
            window.setDecorFitsSystemWindows(false)
            binding.statusBarBg.setOnApplyWindowInsetsListener { view, insets ->
                val systemWindows =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams { height = systemWindows.top }
                return@setOnApplyWindowInsetsListener insets
            }
            binding.navigationBarBg.setOnApplyWindowInsetsListener { view, insets ->
                val systemWindows =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams { height = systemWindows.bottom }
                return@setOnApplyWindowInsetsListener insets
            }
        }, belowSdk = {
            WindowCompat.setDecorFitsSystemWindows(window, false)
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
        })

        window.setBackgroundDrawableResource(R.color.first)

        destinationChangedListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
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

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        destinationChangedListener?.let {
            navController?.addOnDestinationChangedListener(it)
        }

    }

    override fun hideSystemBar() {
        sdkAndUp(Build.VERSION_CODES.S, onSdkAndAbove = {
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        }, belowSdk = {
            WindowInsetsControllerCompat(window, binding.root).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        })

    }

    override fun showSystemBar() {
        sdkAndUp(Build.VERSION_CODES.S, onSdkAndAbove = {
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
            }
        }, belowSdk = {
            WindowInsetsControllerCompat(
                window,
                binding.root
            ).show(WindowInsetsCompat.Type.systemBars())
        })
    }

    override fun showSnackBarMessage(
        messageText: String,
        buttonText: String?,
        onClick: (() -> Unit)?
    ) {
        showSnackBar(messageText = messageText, buttonText = buttonText, onClick = onClick)
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
        sdkAndUp(Build.VERSION_CODES.R, onSdkAndAbove = {
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }, belowSdk = {
            WindowInsetsControllerCompat(window, binding.root).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        })
    }

    private fun setStatusBarContrast() {
        sdkAndUp(Build.VERSION_CODES.R, onSdkAndAbove = {
            window.insetsController?.setSystemBarsAppearance(
                if (!applicationContext.isDarkThemeOn()) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            window.insetsController?.setSystemBarsAppearance(
                if (!applicationContext.isDarkThemeOn()) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }, belowSdk = {
            WindowInsetsControllerCompat(window, binding.root).apply {
                isAppearanceLightStatusBars = !applicationContext.isDarkThemeOn()
                isAppearanceLightNavigationBars = !applicationContext.isDarkThemeOn()
            }
        })
    }

    private fun showSnackBar(messageText: String, buttonText: String? = null, onClick: (() -> Unit)? = null) {
        val snackBar = Snackbar.make(binding.root, "", Snackbar.LENGTH_LONG)
        val customSnackView = SnackBarLayoutBinding.inflate(LayoutInflater.from(this))
        snackBar.view.setBackgroundColor(Color.TRANSPARENT)
        val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
        snackBarLayout.setPadding(0, 0, 0, 0)
        customSnackView.message.text = messageText
        buttonText?.let {
            customSnackView.button.visibility = View.VISIBLE
            customSnackView.button.text = buttonText
            customSnackView.button.setOnClickListener {
                onClick?.invoke()
                snackBar.dismiss()
            }
        }
        snackBarLayout.addView(customSnackView.root, 0)
        snackBar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        destinationChangedListener?.let {
            navController?.removeOnDestinationChangedListener(it)
            destinationChangedListener = null
        }
    }

}