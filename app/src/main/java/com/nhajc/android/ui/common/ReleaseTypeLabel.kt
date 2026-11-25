package com.nhajc.android.ui.common

import androidx.annotation.StringRes
import com.nhajc.android.R
import com.nhajc.android.data.model.ReleaseType

@StringRes
fun ReleaseType.label(): Int = when (this) {
    ReleaseType.ALBUM -> R.string.release_type_album
    ReleaseType.SINGLE -> R.string.release_type_single
    ReleaseType.EP -> R.string.release_type_ep
    ReleaseType.COMPILATION -> R.string.release_type_compilation
    ReleaseType.UNKNOWN -> R.string.release_type_unknown
}
