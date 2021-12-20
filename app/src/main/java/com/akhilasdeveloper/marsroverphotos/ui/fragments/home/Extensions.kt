package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.*
import androidx.core.widget.addTextChangedListener
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSolSelectBinding
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import com.akhilasdeveloper.marsroverphotos.utilities.formatDateToMillis
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDate
import com.google.android.material.datepicker.*

internal fun HomeFragment.setWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.homeAppbar) { _, insets ->
        val systemWindows =
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val layoutParams = (binding.homeToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
        layoutParams?.setMargins(0, 0, 0, systemWindows.bottom)
        binding.homeToolbar.layoutParams = layoutParams
        return@setOnApplyWindowInsetsListener insets
    }

    if (Constants.AD_ENABLED) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.itemAdBanner) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams =
                (binding.itemAdBanner.layoutParams as? ViewGroup.MarginLayoutParams)

            val bottomMargin =
                requireActivity().resources.getDimension(R.dimen.global_window_padding)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + bottomMargin.toInt())

            binding.itemAdBanner.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }
    }

    val recyclerBottomPadding = binding.photoRecycler.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { _, insets ->
        val systemWindows =
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.photoRecycler.updatePadding(bottom = systemWindows.bottom + recyclerBottomPadding)
        return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.homeCollapsingToolbarTop) { _, insets ->
        val systemWindows =
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val layoutParams =
            (binding.homeToolbarTop.layoutParams as? ViewGroup.MarginLayoutParams)
        layoutParams?.setMargins(0, systemWindows.top, 0, 0)
        binding.homeToolbarTop.layoutParams = layoutParams
        return@setOnApplyWindowInsetsListener insets
    }

    val layoutParams = (binding.progress.layoutParams as? ViewGroup.MarginLayoutParams)
    val marginBottom = layoutParams?.bottomMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(binding.progress) { _, insets ->
        val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + marginBottom)
        binding.progress.layoutParams = layoutParams
        return@setOnApplyWindowInsetsListener insets
    }

    val layoutParamsTop = (binding.progressTop.layoutParams as? ViewGroup.MarginLayoutParams)
    val marginTop = layoutParamsTop?.topMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(binding.progressTop) { _, insets ->
        val systemWindows =
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
        layoutParamsTop?.setMargins(0, marginTop + systemWindows.top, 0, 0)
        binding.progressTop.layoutParams = layoutParamsTop
        return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(binding.slideFrame) { _, insets ->
        val systemWindows =
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
        resources.displayMetrics.let { matrics ->
            val bottomAppBarHeight = 76 * matrics.density.toInt()
            val topAppBarHeight = 200 * matrics.density.toInt()

            binding.solSlider.updateLayoutParams { width = matrics.heightPixels - (bottomAppBarHeight + topAppBarHeight) }

            binding.slideFrame.updateLayoutParams { height = matrics.heightPixels - (bottomAppBarHeight + topAppBarHeight) }

            val layoutParams3 =
                (binding.slideFrame.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams3?.bottomMargin = bottomAppBarHeight + systemWindows.bottom
            binding.slideFrame.layoutParams = layoutParams3
        }

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
                if (solSlider.value < solSlider.valueTo)
                    solSlider.value += 1
            }
            prevSolSelector.setOnClickListener {
                if (solSlider.value > 0)
                    solSlider.value -= 1
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