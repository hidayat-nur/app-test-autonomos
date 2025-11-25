package com.appautomation.di

import android.content.Context
import com.appautomation.service.AppLauncher
import com.appautomation.service.AppMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppLauncher(@ApplicationContext context: Context): AppLauncher {
        return AppLauncher(context)
    }
    
    @Provides
    @Singleton
    fun provideAppMonitor(@ApplicationContext context: Context): AppMonitor {
        return AppMonitor(context)
    }
}
