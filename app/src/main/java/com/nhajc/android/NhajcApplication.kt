package com.nhajc.android

import android.app.Application
import com.nhajc.android.data.repository.DownloadRepository
import com.nhajc.android.data.repository.SearchRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NhajcApplication : Application() {
    @Inject lateinit var searchRepository: SearchRepository

    @Inject lateinit var downloadRepository: DownloadRepository

    override fun onCreate() {
        super.onCreate()

        downloadRepository.start()
    }
}
