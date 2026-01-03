package com.krumin.tonguecoinsmanager.di

import com.krumin.tonguecoinsmanager.data.repository.GcsPhotoRepository
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repository
    single<PhotoRepository> { 
        GcsPhotoRepository(
            context = androidContext(),
            bucketName = "tonguecoins" // Hardcoded as per user example, can be moved to config
        ) 
    }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { parameters -> EditPhotoViewModel(get(), photoId = parameters.getOrNull()) }
}
