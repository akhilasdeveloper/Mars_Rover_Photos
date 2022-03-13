package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.*
import androidx.core.widget.addTextChangedListener
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSolSelectBinding
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import com.google.android.material.datepicker.*

internal fun HomeFragment.setWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppbar.homeAppbar) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.bottomAppbar.homeToolbar.updateMarginAndHeight(bottom = systemWindows.bottom)
        binding.bottomAppbar.homeToolbar.updatePadding(right = systemWindows.right, left = systemWindows.left)
        return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.homeBottomToolbarSecond) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.homeBottomToolbarSecond.updateMarginAndHeight(bottom = systemWindows.bottom)
        binding.homeBottomToolbarSecond.updatePadding(right = systemWindows.right, left = systemWindows.left)
        return@setOnApplyWindowInsetsListener insets
    }

    val recyclerBottomPadding = binding.photoRecycler.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.photoRecycler.updatePadding(bottom = systemWindows.bottom + recyclerBottomPadding)
        return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.topAppbar.homeCollapsingToolbarTop) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.topAppbar.homeToolbarTop.updateMarginAndHeight(top = systemWindows.top)
        return@setOnApplyWindowInsetsListener insets
    }

    val marginBottom = binding.progress.marginBottom
    ViewCompat.setOnApplyWindowInsetsListener(binding.progress) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.progress.updateMarginAndHeight(bottom = systemWindows.bottom + marginBottom)
        return@setOnApplyWindowInsetsListener insets
    }

    val marginTop = binding.progressTop.marginTop
    ViewCompat.setOnApplyWindowInsetsListener(binding.progressTop) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.progressTop.updateMarginAndHeight(top = marginTop + systemWindows.top)
        return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.slideFrame) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        val bottomAppBarHeight = toDpi(76)
        val topAppBarHeight = toDpi(200)

        binding.solSlider.updateLayoutParams {
            width = displayHeightPx - (bottomAppBarHeight + topAppBarHeight)
        }
        binding.slideFrame.updateLayoutParams {
            height = displayHeightPx - (bottomAppBarHeight + topAppBarHeight)
        }
        binding.slideFrame.updateMarginAndHeight(bottom = bottomAppBarHeight + systemWindows.bottom, end = systemWindows.right)

        return@setOnApplyWindowInsetsListener insets
    }
}

internal fun HomeFragment.showSolSelectorDialog() {


    val dialogView: LayoutSolSelectBinding =
        LayoutSolSelectBinding.inflate(LayoutInflater.from(requireContext()))
    val builder: AlertDialog.Builder =
        AlertDialog.Builder(requireContext(), R.style.dialog_background)
            .setView(dialogView.root)
    val alertDialog: AlertDialog = builder.create()

    dialogView.apply {
        homeViewModel.getMaxSol()?.let { max_sol ->
            solSlider.valueTo = max_sol.toFloat()
        }
        var solVal = homeViewModel.getSolSelectDialogValue()
        if (solVal == null) {
            homeViewModel.getCurrentDate()?.let { currentDate ->
                homeViewModel.getLandingDateInMillis()?.let { landing_date_in_millis ->
                    utilities.calculateDays(landing_date_in_millis, currentDate)?.let {
                        solVal = it.toFloat()
                    }
                }
            }
        }
        solVal?.let {
            solSlider.value = it
        }
        solSelectorCount.setText(dialogView.solSlider.value.toInt().toString())
        solSlider.addOnChangeListener { _, value, _ ->
            solSelectorCount.setText("${value.toInt()}")
            homeViewModel.setSolSelectDialogValue(value)
        }
        nextSolSelector.setOnClickListener {
            if (solSlider.value < solSlider.valueTo) {
                solSlider.value += 1
                utilities.vibrate()
            }
        }
        prevSolSelector.setOnClickListener {
            if (solSlider.value > 0) {
                solSlider.value -= 1
                utilities.vibrate()
            }
        }
        solSelectorCount.addTextChangedListener { edit ->
            edit?.let {
                val validatedText = homeViewModel.validateSolText(it.toString())
                if (validatedText != edit.toString()) {
                    solSelectorCount.setText(validatedText)
                }
                solSlider.value = validatedText.toInt().toFloat()
            }

        }
        okSolSelector.setOnClickListener {
            onSolSelected(solSlider.value.toLong())
            alertDialog.cancel()
        }

        cancelSolSelector.setOnClickListener {
            alertDialog.cancel()
        }
    }

    alertDialog.setOnDismissListener {
        homeViewModel.setViewStateShowSolSelected(false)
        homeViewModel.setSolSelectDialogValue(null)
    }

    alertDialog.show()

}

internal fun HomeFragment.showDatePicker() {

    val constraintsBuilder = CalendarConstraints.Builder()
    val validators: ArrayList<CalendarConstraints.DateValidator> = ArrayList()
    homeViewModel.getLandingDateInMillis()?.let { landing_date_in_millis ->
        validators.add(DateValidatorPointForward.from(landing_date_in_millis))
    }
    homeViewModel.getMaxDateInMillis()?.let { max_date_in_millis ->
        validators.add(DateValidatorPointBackward.before(max_date_in_millis + MILLIS_IN_A_DAY))
    }
    constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators))

    val setDate = homeViewModel.getCurrentDate()?.let { it + MILLIS_IN_A_DAY }
    val datePicker =
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(setDate)
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

    datePicker.show(requireActivity().supportFragmentManager, "RoverDatePicker")

    datePicker.addOnPositiveButtonClickListener {
        datePicker.selection?.let {
            onDateSelected(it.formatMillisToDate().formatDateToMillis()!!, true)
        }
    }

    datePicker.addOnDismissListener {
        homeViewModel.setViewStateShowDatePicket(false)
    }
}
