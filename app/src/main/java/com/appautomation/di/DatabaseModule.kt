package com.appautomation.di

import android.content.Context
import com.appautomation.data.local.AppDatabase
import com.appautomation.data.local.AutomationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideAutomationDao(database: AppDatabase): AutomationDao {
        return database.automationDao()
    }
}
