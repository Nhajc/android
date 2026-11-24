package com.nhajc.android

import android.app.Application
import com.nhajc.android.data.repository.SearchSession

class NhajcApplication : Application() {
    val searchSession by lazy { SearchSession() }
}
