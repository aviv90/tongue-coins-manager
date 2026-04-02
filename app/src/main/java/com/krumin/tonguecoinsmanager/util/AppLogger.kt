package com.krumin.tonguecoinsmanager.util

import android.util.Log
import com.krumin.tonguecoinsmanager.BuildConfig

object AppLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
