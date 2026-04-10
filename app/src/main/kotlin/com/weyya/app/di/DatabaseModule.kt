package com.weyya.app.di

import android.content.Context
import androidx.room.Room
import com.weyya.app.data.db.WeyYaDatabase
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.dao.WhitelistDao
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
    fun provideDatabase(@ApplicationContext context: Context): WeyYaDatabase =
        Room.databaseBuilder(
            context,
            WeyYaDatabase::class.java,
            "weyya.db",
        )
            .addMigrations(WeyYaDatabase.MIGRATION_1_2, WeyYaDatabase.MIGRATION_2_3, WeyYaDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun provideBlockedCallDao(db: WeyYaDatabase): BlockedCallDao =
        db.blockedCallDao()

    @Provides
    fun provideScheduleDao(db: WeyYaDatabase): ScheduleDao =
        db.scheduleDao()

    @Provides
    fun provideWhitelistDao(db: WeyYaDatabase): WhitelistDao =
        db.whitelistDao()
}
