package com.akhilasdeveloper.marsroverphotos.ui

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface UICommunicationListener {
    fun hideSystemBar()
    fun showSystemBar()
    fun showSnackBarMessage(
        messageText: String,
        buttonText: String? = null,
        onClick: (() -> Unit)? = null
    )

    fun showIndeterminateProgressDialog(isCancelable: Boolean = false, onCancelSelect: (() -> Unit)? = null)
    fun hideIndeterminateProgressDialog()
    fun showShareSelectorDialog(onImageSelect: () -> Unit, onLinkSelect: () -> Unit)
    fun showMoreSelectorDialog(
        onImageSelect: () -> Unit,
        onLinkSelect: () -> Unit,
        onDownloadSelect: () -> Unit,
        onDeleteSelect: ((marsRoverPhotoTable: MarsRoverPhotoTable, position: Int) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        items: List<MarsRoverPhotoTable>? = null
    )

    fun setInfoDetails(marsRoverPhotoTable: MarsRoverPhotoTable)
    fun showInfoDialog(marsRoverPhotoTable: MarsRoverPhotoTable? = null)
    fun closeInfoDialog()
    fun showDownloadProgressDialog(progress: Int, onCancelClicked: () -> Unit)
    fun hideDownloadProgressDialog()
    fun showConsentSelectorDialog(
        title: String = "Info",
        descriptionText: String,
        oKText: String = "OK",
        doNotShow: Boolean = false,
        cancelText: String = "Cancel",
        onOkSelect: (doNotShow:Boolean) -> Unit,
        onCancelSelect: ((doNotShow:Boolean) -> Unit)? = null
    )
}