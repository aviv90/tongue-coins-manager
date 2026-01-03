package com.krumin.tonguecoinsmanager

import android.app.Application
import com.krumin.tonguecoinsmanager.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TongueCoinsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TongueCoinsApplication)
            modules(appModule)
        }
    }
}
