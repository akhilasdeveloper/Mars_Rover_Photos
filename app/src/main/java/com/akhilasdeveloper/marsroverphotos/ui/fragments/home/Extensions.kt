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
        return@setOnApplyWindowInsetsListener insets
    }

    if (Constants.AD_ENABLED) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.adView.itemAdBanner) { _, insets ->
            val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomMargin = requireActivity().resources.getDimension(R.dimen.global_window_padding)
            binding.adView.itemAdBanner.updateMarginAndHeight(bottom = systemWindows.bottom + bottomMargin.toInt())
            return@setOnApplyWindowInsetsListener insets
        }
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.homeBottomToolbarSecond) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.homeBottomToolbarSecond.updateMarginAndHeight(bottom = systemWindows.bottom)
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

        binding.solSlider.updateLayoutParams { width = displayHeightPx - (bottomAppBarHeight + topAppBarHeight) }
        binding.slideFrame.updateLayoutParams { height = displayHeightPx - (bottomAppBarHeight + topAppBarHeight) }
        binding.slideFrame.updateMarginAndHeight(bottom = bottomAppBarHeight + systemWindows.bottom)

        return@setOnApplyWindowInsetsListener insets
    }
}

internal fun HomeFragment.showSolSelectorDialog() {
    master?.let { rover ->

        val dialogView: LayoutSolSelectBinding =
            LayoutSolSelectBinding.inflate(LayoutInflater.from(requireContext()))
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(requireContext(), R.style.dialog_background)
                .setView(dialogView.root)
        val alertDialog: AlertDialog = builder.create()

        dialogView.apply {
            solSlider.valueTo = rover.max_sol.toFloat()
            utilities.calculateDays(rover.landing_date_in_millis, currentDate)?.let {
                solSlider.value = it.toFloat()
            }
            solSelectorCount.setText(dialogView.solSlider.value.toInt().toString())
            solSlider.addOnChangeListener { _, value, _ ->
                solSelectorCount.setText("${value.toInt()}")
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
                    val validatedText = validateSolText(it.toString())
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

        alertDialog.show()
    }
}

internal fun HomeFragment.showDatePicker() {
    master?.let { master ->

        val constraintsBuilder = CalendarConstraints.Builder()
        val validators: ArrayList<CalendarConstraints.DateValidator> = ArrayList()
        validators.add(DateValidatorPointForward.from(master.landing_date_in_millis))
        validators.add(DateValidatorPointBackward.before(master.max_date_in_millis + MILLIS_IN_A_DAY))
        constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators))

        val setDate = currentDate?.let { it + MILLIS_IN_A_DAY }
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
    }
}

private fun HomeFragment.validateSolText(it: String): String {
    var validated = it.filter { str -> str.isDigit() }
    if (validated.isNotEmpty()) {
        val sol = validated.toInt()
        master?.let { rover ->
            if (rover.max_sol < sol)
                validated = rover.max_sol.toString()
            if (sol < 0)
                validated = "0"
        }
    }
    return validated
}