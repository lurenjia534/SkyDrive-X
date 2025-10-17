package com.lurenjia534.skydrivex.di

import android.content.Context
import androidx.room.Room
import com.lurenjia534.skydrivex.data.local.index.IndexDao
import com.lurenjia534.skydrivex.data.local.index.IndexDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IndexModule {

    @Provides
    @Singleton
    fun provideIndexDatabase(@ApplicationContext context: Context): IndexDatabase =
        Room.databaseBuilder(context, IndexDatabase::class.java, "index.db").build()

    @Provides
    @Singleton
    fun provideIndexDao(database: IndexDatabase): IndexDao = database.indexDao()
}

