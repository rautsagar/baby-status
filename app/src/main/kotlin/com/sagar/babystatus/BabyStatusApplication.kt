package com.sagar.babystatus

import android.app.Application
import com.sagar.babystatus.core.model.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class BabyStatusApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun providesAppInfo() = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        feedbackEmail = BuildConfig.FEEDBACK_EMAIL,
    )
}
