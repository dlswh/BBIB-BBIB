package com.s14p31s207.android.data.di // 적절한 패키지에 위치시켜줘

import android.content.Context
import com.s14p31s207.android.data.db.CommandRequestDao
import com.s14p31s207.android.data.db.CommandSequenceDao
import com.s14p31s207.android.data.db.AppDatabase
import com.s14p31s207.android.data.db.LogDao
import com.s14p31s207.android.data.repository.CommandSequenceRepositoryImpl
import com.s14p31s207.android.data.repository.LogRepositoryImpl
import com.s14p31s207.android.domain.repository.CommandSequenceRepository
import com.s14p31s207.android.domain.repository.LogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.s14p31s207.android.data.db.IpcDeviceDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWifiDatabase(@ApplicationContext context: Context): AppDatabase {
        // 네가 만든 WifiDatabase의 getInstance를 여기서 호출해주는 거야!
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideIpcDeviceDao(database: AppDatabase): IpcDeviceDao {
        return database.ipcDeviceDao()
    }

    @Provides
    fun provideCommandSequenceDao(database: AppDatabase): CommandSequenceDao {
        return database.commandSequenceDao()
    }

    @Provides
    fun provideCommandRequestDao(database: AppDatabase): CommandRequestDao {
        return database.commandRequestDao()
    }

    @Provides
    @Singleton
    fun provideCommandSequenceRepository(
        commandSequenceDao: CommandSequenceDao
    ): CommandSequenceRepository {
        return CommandSequenceRepositoryImpl(commandSequenceDao)
    }

    @Provides
    @Singleton
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()

    @Provides
    @Singleton
    fun provideLogRepository(logDao: LogDao): LogRepository =
        LogRepositoryImpl(logDao)
}
