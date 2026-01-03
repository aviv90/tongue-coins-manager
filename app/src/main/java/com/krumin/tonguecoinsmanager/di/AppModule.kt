package com.krumin.tonguecoinsmanager.di

import com.krumin.tonguecoinsmanager.data.repository.GcsPhotoRepository
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repository
    single<PhotoRepository> {
        GcsPhotoRepository(
            context = androidContext(),
            bucketName = "tonguecoins"
        )
    }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { parameters -> EditPhotoViewModel(get(), photoId = parameters.getOrNull()) }
}
