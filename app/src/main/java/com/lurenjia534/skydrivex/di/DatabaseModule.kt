package com.lurenjia534.skydrivex.di

import android.content.Context
import androidx.room.Room
import com.lurenjia534.skydrivex.data.local.auth.AuthConfigDao
import com.lurenjia534.skydrivex.data.local.auth.AuthConfigDatabase
import com.lurenjia534.skydrivex.data.local.db.TransferDatabase
import com.lurenjia534.skydrivex.data.local.db.dao.TransferDao
import com.lurenjia534.skydrivex.data.local.db.TransferRepository
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTransferDatabase(
        @ApplicationContext context: Context
    ): TransferDatabase = Room.databaseBuilder(
        context,
        TransferDatabase::class.java,
        "transfer_db"
    ).build()

    @Provides
    fun provideTransferDao(database: TransferDatabase): TransferDao = database.transferDao()

    @Provides
    @Singleton
    fun provideAuthConfigDatabase(
        @ApplicationContext context: Context
    ): AuthConfigDatabase = Room.databaseBuilder(
        context,
        AuthConfigDatabase::class.java,
        "auth_config_db"
    ).build()

    @Provides
    fun provideAuthConfigDao(database: AuthConfigDatabase): AuthConfigDao = database.authConfigDao()

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideTransferTracker(repository: TransferRepository): TransferTracker {
        TransferTracker.initialize(repository)
        return TransferTracker
    }
}
