package com.krumin.tonguecoinsmanager.di

import androidx.room.Room
import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.krumin.tonguecoinsmanager.BuildConfig
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.local.AppDatabase
import com.krumin.tonguecoinsmanager.data.local.DatabaseMigrations
import com.krumin.tonguecoinsmanager.data.repository.BroadcastRepositoryImpl
import com.krumin.tonguecoinsmanager.data.repository.DailyRiddleRepositoryImpl
import com.krumin.tonguecoinsmanager.data.repository.FcmDraftRepository
import com.krumin.tonguecoinsmanager.data.repository.FcmRepositoryImpl
import com.krumin.tonguecoinsmanager.data.repository.GcsPhotoRepository
import com.krumin.tonguecoinsmanager.data.service.GeminiCategoryGenerator
import com.krumin.tonguecoinsmanager.data.service.GeminiFcmGenerator
import com.krumin.tonguecoinsmanager.data.service.GeminiImageEditor
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.domain.service.FcmGenerator
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.GetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.ResetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.SetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.GenerateFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.ManageTestDevicesUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.ScheduleFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.SendFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.ui.viewmodel.BroadcastViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.DailyRiddleViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.threeten.bp.Duration
import java.util.concurrent.TimeUnit

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppConfig.Persistence.ROOM_DATABASE_NAME
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()
    }

    single { get<AppDatabase>().pendingChangeDao() }
    single { get<AppDatabase>().testDeviceDao() }

    // Network
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

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

    single<FcmRepository> {
        FcmRepositoryImpl(
            context = androidContext(),
            firestore = get(),
            testDeviceDao = get(),
            okHttpClient = get()
        )
    }

    // Gemini
    single<CategoryGenerator> {
        GeminiCategoryGenerator(androidContext(), BuildConfig.GEMINI_API_KEY)
    }

    single<FcmGenerator> {
        GeminiFcmGenerator(BuildConfig.GEMINI_API_KEY)
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
    single { SendFcmNotificationUseCase(get()) }
    single { ScheduleFcmNotificationUseCase(get()) }
    single { GenerateFcmNotificationUseCase(get()) }
    single { ManageTestDevicesUseCase(get()) }
    single { FcmDraftRepository(androidContext()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { BroadcastViewModel(get()) }
    viewModel { FcmViewModel(get(), get(), get(), get(), get(), get()) }
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
