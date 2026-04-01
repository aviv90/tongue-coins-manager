package com.krumin.tonguecoinsmanager.di

import androidx.room.Room
import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.krumin.tonguecoinsmanager.BuildConfig
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.local.AppDatabase
import com.krumin.tonguecoinsmanager.data.repository.BroadcastRepositoryImpl
import com.krumin.tonguecoinsmanager.data.repository.DailyRiddleRepositoryImpl
import com.krumin.tonguecoinsmanager.data.repository.GcsPhotoRepository
import com.krumin.tonguecoinsmanager.data.service.GeminiCategoryGenerator
import com.krumin.tonguecoinsmanager.data.service.GeminiImageEditor
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.GetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.ResetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.SetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.ui.viewmodel.BroadcastViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.DailyRiddleViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.threeten.bp.Duration

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "tongue_coins_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().pendingChangeDao() }
    single { get<AppDatabase>().testDeviceDao() }

    // Network
    single { OkHttpClient() }

    // Firestore - Centralized & Resilient
    single<Firestore> {
        val context = androidContext()
        val retrySettings = RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(500))
            .setRetryDelayMultiplier(1.5)
            .setMaxRetryDelay(Duration.ofMillis(5000))
            .setTotalTimeout(Duration.ofMinutes(1))
            .build()

        val options = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(context.assets.open(AppConfig.Gcs.DEFAULT_KEY_FILE)))
            .setDatabaseId(AppConfig.Firestore.DATABASE_ID)
            .setRetrySettings(retrySettings)
            .build()
        options.service
    }

    // Repository
    single<PhotoRepository> {
        GcsPhotoRepository(
            context = androidContext(),
            pendingChangeDao = get(),
            privateBucketName = AppConfig.Gcs.PRIVATE_BUCKET,
            publicBucketName = AppConfig.Gcs.PUBLIC_BUCKET
        )
    }

    single<com.krumin.tonguecoinsmanager.domain.repository.FcmRepository> {
        com.krumin.tonguecoinsmanager.data.repository.FcmRepositoryImpl(
            context = androidContext(),
            testDeviceDao = get(),
            okHttpClient = get()
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
        DailyRiddleRepositoryImpl(androidContext(), get())
    }

    single<BroadcastRepository> {
        BroadcastRepositoryImpl(androidContext(), get())
    }

    // Daily Riddle Use Cases
    single { GetDailyRiddleUseCase(get()) }
    single { SetDailyRiddleUseCase(get()) }
    single { ResetDailyRiddleUseCase(get()) }

    // FCM Use Cases
    single { com.krumin.tonguecoinsmanager.domain.usecase.fcm.SendFcmNotificationUseCase(get()) }
    single { com.krumin.tonguecoinsmanager.domain.usecase.fcm.ManageTestDevicesUseCase(get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { BroadcastViewModel(get()) }
    viewModel { FcmViewModel(get(), get()) }
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
