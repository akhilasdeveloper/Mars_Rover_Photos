package com.akhilasdeveloper.marsroverphotos.ui

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.*
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.ActivityMainBinding
import com.akhilasdeveloper.marsroverphotos.isDarkThemeOn
import com.akhilasdeveloper.marsroverphotos.showShortToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var statusBarCallBack: WindowInsetsAnimationCompat.Callback? = null
    private var navigationBarCallBack: WindowInsetsAnimationCompat.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(R.color.first)


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

        statusBarCallBack = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {

            var heightPre = 0f
            var heightPost = 0f

            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                heightPre = binding.statusBarBg.bottom.toFloat()
            }

            override fun onStart(
                animation: WindowInsetsAnimationCompat,
                bounds: WindowInsetsAnimationCompat.BoundsCompat
            ): WindowInsetsAnimationCompat.BoundsCompat {
                heightPost = binding.statusBarBg.bottom.toFloat()
                return super.onStart(animation, bounds)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                val sysBarAnimation =
                    runningAnimations.find { it.typeMask and WindowInsetsCompat.Type.statusBars() != 0 }
                        ?: return insets
                binding.statusBarBg.translationY =
                    (heightPre - heightPost) * (1 - sysBarAnimation.interpolatedFraction)
                return insets
            }
        }

        ViewCompat.setWindowInsetsAnimationCallback(binding.statusBarBg, statusBarCallBack)
        navigationBarCallBack = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {

            var heightPre = 0f
            var heightPost = 0f

            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                heightPre = binding.navigationBarBg.top.toFloat()
            }

            override fun onStart(
                animation: WindowInsetsAnimationCompat,
                bounds: WindowInsetsAnimationCompat.BoundsCompat
            ): WindowInsetsAnimationCompat.BoundsCompat {
                heightPost = binding.navigationBarBg.top.toFloat()
                return super.onStart(animation, bounds)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                val sysBarAnimation =
                    runningAnimations.find { it.typeMask and WindowInsetsCompat.Type.statusBars() != 0 }
                        ?: return insets
                binding.navigationBarBg.translationY =
                    (heightPre - heightPost) * (1 - sysBarAnimation.interpolatedFraction)
                return insets
            }
        }

        ViewCompat.setWindowInsetsAnimationCallback(binding.navigationBarBg, navigationBarCallBack)

        setStatusBarContrast()
    }

    override fun onDestroy() {
        super.onDestroy()
        statusBarCallBack = null
        navigationBarCallBack = null
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

    override fun setTransparentSystemBar() {
        binding.navigationBarBg.isVisible = false
        binding.statusBarBg.isVisible = false

    }

    override fun removeTransparentSystemBar() {
        binding.navigationBarBg.isVisible = true
        binding.statusBarBg.isVisible = true
    }

    override fun setStatusBarTheme() {
        setStatusBarContrast()
    }

    private fun setStatusBarContrast() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !applicationContext.isDarkThemeOn()
            isAppearanceLightNavigationBars = false
        }
    }

}