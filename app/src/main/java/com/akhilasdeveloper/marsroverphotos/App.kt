package com.akhilasdeveloper.marsroverphotos

import android.app.Application
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
    }
}