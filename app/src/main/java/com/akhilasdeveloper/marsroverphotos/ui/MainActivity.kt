package com.akhilasdeveloper.marsroverphotos.ui

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.akhilasdeveloper.marsroverphotos.databinding.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerShareClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.bumptech.glide.RequestManager

import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null
    private var navController: NavController? = null
    private var alertDialog: AlertDialog? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var dialogView: LayoutProgressBinding? = null
    @Inject
    lateinit var requestManager: RequestManager
    @Inject
    lateinit var utilities: Utilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val viewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                viewModel.setInfoDialog(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })
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
        alertDialog?.dismiss()
    }

    override fun showMoreSelectorDialog(
        onImageSelect: () -> Unit,
        onLinkSelect: () -> Unit,
        onDownloadSelect: () -> Unit,
        onDeleteSelect: ((marsRoverPhotoTable: MarsRoverPhotoTable, position: Int) -> Unit)?,
        onDismiss: (() -> Unit)?,
        items: List<MarsRoverPhotoTable>?
    ) {
        val bottomSheetDialog = BottomSheetDialog(this,R.style.ShareBottomSheetTheme)
        val dialogView: LayoutShareBottomSheetBinding = LayoutShareBottomSheetBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(dialogView.root)
        val data = items?.toMutableList()
        var shareAdapter:ShareRecyclerAdapter? = null
        val listener = object :RecyclerShareClickListener{
            override fun onItemDeleteClicked(
                marsRoverPhotoTable: MarsRoverPhotoTable,
                position: Int
            ) {
                onDeleteSelect?.invoke(marsRoverPhotoTable, position)
                data?.removeAt(position)
                data?.let {
                    shareAdapter?.submitList(it)
                }
                shareAdapter?.notifyItemRemoved(position)
                dialogView.selectCount.text = getString(R.string.selected,data?.size.toString() )
                if (data?.isEmpty() == true)
                    bottomSheetDialog.dismiss()
            }
        }
        shareAdapter = ShareRecyclerAdapter(requestManager = requestManager, interaction = listener)
        dialogView.apply {

            shareItems.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.HORIZONTAL,false)
            shareItems.adapter = shareAdapter

            link.setOnClickListener {
                onLinkSelect()
                bottomSheetDialog.cancel()
            }

            image.setOnClickListener {
                onImageSelect()
                bottomSheetDialog.cancel()
            }

            download.setOnClickListener {
                onDownloadSelect()
                bottomSheetDialog.cancel()
            }

            selectCount.text = getString(R.string.selected,data?.size.toString())
        }
        data?.let {
            shareAdapter.submitList(it)
        }
        bottomSheetDialog.setOnDismissListener {
            onDismiss?.invoke()
        }
        bottomSheetDialog.show()

    }

    override fun setInfoDetails(marsRoverPhotoTable: MarsRoverPhotoTable) {
        binding.layoutInfoBottomSheet.apply {
            imageId.text = marsRoverPhotoTable.photo_id.toString()
            cameraName.text = getString(R.string.camera_name,marsRoverPhotoTable.camera_full_name,marsRoverPhotoTable.camera_name )
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
        title: String?,
        descriptionText: String,
        oKText: String?,
        doNotShow: Boolean,
        cancelText: String?,
        onOkSelect: (doNotShow: Boolean) -> Unit,
        onCancelSelect: ((doNotShow: Boolean) -> Unit)?,
        onDismiss: (() -> Unit)?
    ) {
        val dialogView: LayoutConsentBinding =
            LayoutConsentBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        dialogView.apply {

            this.title.text = title?:getString(R.string.info)
            this.cancel.text = cancelText?:getString(R.string.cancel)
            this.description.text = descriptionText
            this.ok.text = oKText?:getString(R.string.ok)

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

        alertDialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        alertDialog.show()

    }

    override fun showAboutDialog(
        onDismiss: (() -> Unit)?
    ) {
        val dialogView: LayoutAboutBinding =
            LayoutAboutBinding.inflate(LayoutInflater.from(this))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        val apiDescription = "<a href='https://api.nasa.gov/' > api.nasa.gov </a>"
        val contactDescription = "<a href='mailto:akhilasdeveloper@gmail.com' > akhilasdeveloper@gmail.com </a>"

        dialogView.apply {
            sdkAndUp(Build.VERSION_CODES.N, onSdkAndAbove = {
                apiProvider.text = Html.fromHtml(apiDescription,Html.FROM_HTML_MODE_COMPACT)
                contact.text = Html.fromHtml(contactDescription,Html.FROM_HTML_MODE_COMPACT)
            }, belowSdk = {
                apiProvider.text = Html.fromHtml(apiDescription)
            })

            clearCache.setOnClickListener {
                utilities.deleteCache()
            }
        }

        alertDialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        alertDialog.show()

    }

    override fun showDownloadProgressDialog(progress: Int, onCancelClicked: () -> Unit) {
        if (alertDialog?.isShowing != true) {
            showDownloadDialog {
                alertDialog?.cancel()
                onCancelClicked()
            }
        }
        dialogView?.progress?.progress = progress
        dialogView?.progressCount?.text = getString(R.string.progress, progress.toString())
    }

    override fun hideDownloadProgressDialog() {
        alertDialog?.cancel()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        else {
            super.onBackPressed()
        }
    }


}