package com.akhilasdeveloper.marsroverphotos.ui

interface UICommunicationListener {
    fun hideSystemBar()
    fun showSystemBar()
    fun showSnackBarMessage(messageText: String, buttonText: String? = null, onClick: (() -> Unit)? = null)
    fun showShareSelectorDialog(onImageSelect: () -> Unit, onLinkSelect: () -> Unit)
}