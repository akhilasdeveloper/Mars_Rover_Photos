package com.akhilasdeveloper.marsroverphotos.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AlertDialog
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.akhilasdeveloper.marsroverphotos.databinding.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.HomeFragment
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.akhilasdeveloper.marsroverphotos.utilities.isDarkThemeOn
import com.akhilasdeveloper.marsroverphotos.utilities.sdkAndUp
import com.akhilasdeveloper.marsroverphotos.utilities.updateMarginAndHeight
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.akhilasdeveloper.marsroverphotos.R


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null
    private var navController: NavController? = null
    private var alertDialog: AlertDialog? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var dialogView: LayoutProgressBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this)

        sdkAndUp(Build.VERSION_CODES.R, onSdkAndAbove = {
            window.setDecorFitsSystemWindows(false)
            binding.statusBarBg.setOnApplyWindowInsetsListener { view, insets ->
                if (view.height == 0) {
                    val systemWindows =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updateLayoutParams { height = systemWindows.top }
                }
                return@setOnApplyWindowInsetsListener insets
            }
            binding.navigationBarBg.setOnApplyWindowInsetsListener { view, insets ->
                if (view.height == 0) {
                    val systemWindows =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updateLayoutParams { height = systemWindows.bottom }
                }
                return@setOnApplyWindowInsetsListener insets
            }
        }, belowSdk = {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarBg) { view, insets ->
                if (view.height==0) {
                    val systemWindows =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updateLayoutParams { height = systemWindows.top }
                }
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.navigationBarBg) { view, insets ->
                if (view.height==0) {
                    val systemWindows =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updateLayoutParams { height = systemWindows.bottom }
                }
                return@setOnApplyWindowInsetsListener insets
            }
        })

        val marginBottom = binding.layoutInfoBottomSheet.root.marginBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutInfoBottomSheet.root) { _, insets ->
            val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutInfoBottomSheet.root.updateMarginAndHeight(bottom = systemWindows.bottom + marginBottom)
            return@setOnApplyWindowInsetsListener insets
        }

        window.setBackgroundDrawableResource(R.color.first)

        setBottomSheet()

        destinationChangedListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                closeInfoDialog()

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

        binding.layoutInfoBottomSheet.close.setOnClickListener {
            closeInfoDialog()
        }

    }

    private fun setBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val viewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
                viewModel.setInfoDialog(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })
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

    private fun showSnackBar(
        messageText: String,
        buttonText: String? = null,
        onClick: (() -> Unit)? = null
    ) {
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

    override fun showShareSelectorDialog(onImageSelect: () -> Unit, onLinkSelect: () -> Unit) {
        val dialogView: LayoutShareSelectBinding =
            LayoutShareSelectBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        dialogView.apply {

            linkSelect.setOnClickListener {
                onLinkSelect()
                alertDialog.cancel()
            }

            imageSelect.setOnClickListener {
                onImageSelect()
                alertDialog.cancel()
            }

            cancelSolSelector.setOnClickListener {
                alertDialog.cancel()
            }
        }

        alertDialog.show()

    }

    override fun showMoreSelectorDialog(
        onImageSelect: () -> Unit,
        onLinkSelect: () -> Unit,
        onDownloadSelect: () -> Unit
    ) {
        val dialogView: LayoutMoreSelectBinding =
            LayoutMoreSelectBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        dialogView.apply {

            layoutMoreSelectContent.apply {
                linkSelect.setOnClickListener {
                    onLinkSelect()
                    alertDialog.cancel()
                }

                imageSelect.setOnClickListener {
                    onImageSelect()
                    alertDialog.cancel()
                }

                downloadSelect.setOnClickListener {
                    onDownloadSelect()
                    alertDialog.cancel()
                }
            }

            cancelSolSelector.setOnClickListener {
                alertDialog.cancel()
            }
        }

        alertDialog.show()

    }

    override fun setInfoDetails(marsRoverPhotoTable: MarsRoverPhotoTable) {
        binding.layoutInfoBottomSheet.apply {
            imageId.text = marsRoverPhotoTable.photo_id.toString()
            cameraName.text =
                "${marsRoverPhotoTable.camera_full_name} (${marsRoverPhotoTable.camera_name})"
            roverName.text = marsRoverPhotoTable.rover_name
            date.text = marsRoverPhotoTable.earth_date.formatMillisToDisplayDate()
            sol.text = marsRoverPhotoTable.sol.toString()
        }
    }

    override fun showInfoDialog(marsRoverPhotoTable: MarsRoverPhotoTable?) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        marsRoverPhotoTable?.let {
            setInfoDetails(it)
        }
    }

    override fun closeInfoDialog() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun showIndeterminateProgressDialog(
        isCancelable: Boolean,
        onCancelSelect: (() -> Unit)?
    ) {
        binding.layoutProgressIndeterminate.root.isVisible = true

    }

    override fun hideIndeterminateProgressDialog() {
        binding.layoutProgressIndeterminate.root.isVisible = false
    }

    private fun showDownloadDialog(onCancelClicked: () -> Unit) {
        dialogView = LayoutProgressBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView?.root)
        alertDialog = builder.create()
        alertDialog?.setCanceledOnTouchOutside(false)
        dialogView?.apply {
            cancelSolSelector.setOnClickListener {
                onCancelClicked()
            }
        }
        alertDialog?.show()
    }

    override fun showConsentSelectorDialog(
        title: String,
        descriptionText: String,
        oKText: String,
        doNotShow: Boolean,
        cancelText: String,
        onOkSelect: (doNotShow: Boolean) -> Unit,
        onCancelSelect: ((doNotShow: Boolean) -> Unit)?
    ) {
        val dialogView: LayoutConsentBinding =
            LayoutConsentBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        dialogView.apply {

            this.title.text = title
            this.cancel.text = cancelText
            this.description.text = descriptionText
            this.ok.text = oKText

            this.doNotShow.isVisible = doNotShow

            ok.setOnClickListener {
                alertDialog.cancel()
                onOkSelect(this.doNotShow.isChecked)
            }

            cancel.setOnClickListener {
                onCancelSelect?.invoke(this.doNotShow.isChecked)
                alertDialog.cancel()
            }
        }

        alertDialog.show()

    }

    override fun showDownloadProgressDialog(progress: Int, onCancelClicked: () -> Unit) {
        if (alertDialog?.isShowing != true) {
            showDownloadDialog {
                onCancelClicked()
            }
        }
        dialogView?.progress?.progress = progress
        dialogView?.progressCount?.text = "$progress%"
    }

    override fun hideDownloadProgressDialog() {
        alertDialog?.cancel()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        else {
            super.onBackPressed()
/*            showConsentSelectorDialog(getString(R.string.quit),getString(R.string.exit_consent), onOkSelect = {
            })*/
        }
    }
}