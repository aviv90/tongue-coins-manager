package com.krumin.tonguecoinsmanager.util

import com.krumin.tonguecoinsmanager.R
import kotlinx.coroutines.CancellationException
import java.io.FileNotFoundException
import java.io.IOException

fun Throwable.toFriendlyUiText(): UiText {
    if (this is CancellationException) throw this
    return when (this) {
        is FileNotFoundException -> UiText.StringResource(R.string.error_generic_friendly)
        is IOException -> UiText.StringResource(R.string.error_network_friendly)
        else -> UiText.StringResource(R.string.error_generic_friendly)
    }
}
