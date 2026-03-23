package com.krumin.tonguecoinsmanager.di

import androidx.room.Room
import com.krumin.tonguecoinsmanager.BuildConfig
import com.krumin.tonguecoinsmanager.data.local.AppDatabase
import com.krumin.tonguecoinsmanager.data.repository.GcsPhotoRepository
import com.krumin.tonguecoinsmanager.data.service.GeminiCategoryGenerator
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import com.krumin.tonguecoinsmanager.data.service.GeminiImageEditor
import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository
import com.krumin.tonguecoinsmanager.data.repository.DailyRiddleRepositoryImpl
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import com.krumin.tonguecoinsmanager.data.repository.BroadcastRepositoryImpl
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.GetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.SetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.ResetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.ui.viewmodel.BroadcastViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.DailyRiddleViewModel

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "tongue_coins_db"
        ).build()
    }

    single { get<AppDatabase>().pendingChangeDao() }

    // Repository
    single<PhotoRepository> {
        GcsPhotoRepository(
            context = androidContext(),
            pendingChangeDao = get(),
            privateBucketName = AppConfig.Gcs.PRIVATE_BUCKET,
            publicBucketName = AppConfig.Gcs.PUBLIC_BUCKET
        )
    }

    // Gemini
    single<CategoryGenerator> {
        GeminiCategoryGenerator(androidContext(), BuildConfig.GEMINI_API_KEY)
    }

    single<ImageEditor> {
        GeminiImageEditor(
            androidContext(),
            BuildConfig.GEMINI_API_KEY
        )
    }

    single<DailyRiddleRepository> {
        DailyRiddleRepositoryImpl(androidContext())
    }

    // Broadcast Use Cases (none needed if accessing repo directly, but maintaining pattern to inject repo)
    single<BroadcastRepository> {
        BroadcastRepositoryImpl(androidContext())
    }

    // Daily Riddle Use Cases
    single { GetDailyRiddleUseCase(get()) }
    single { SetDailyRiddleUseCase(get()) }
    single { ResetDailyRiddleUseCase(get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { BroadcastViewModel(get()) }
    viewModel {
        DailyRiddleViewModel(
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { parameters ->
        EditPhotoViewModel(
            repository = get(),
            categoryGenerator = get(),
            imageEditor = get(),
            photoId = parameters.getOrNull<String>()
        )
    }
}


