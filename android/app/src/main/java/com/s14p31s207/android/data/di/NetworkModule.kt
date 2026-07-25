package com.s14p31s207.android.data.di

import com.google.gson.Gson
import com.s14p31s207.android.data.comm.api.AuthApi
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionManager
import com.s14p31s207.android.data.comm.tcp.TcpChannel
import com.s14p31s207.android.data.db.CommandRequestDao
import com.s14p31s207.android.data.db.IpcDeviceDao
import com.s14p31s207.android.data.repository.CommandRepositoryImpl
import com.s14p31s207.android.data.repository.ConnectRepositoryImpl
import com.s14p31s207.android.domain.repository.CommandRepository
import com.s14p31s207.android.domain.repository.ConnectRepository
import com.s14p31s207.android.domain.repository.LogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTcpChannel(): TcpChannel = TcpChannel()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideConnectRepository(
        tcpChannel: TcpChannel
    ): ConnectRepository =
        ConnectRepositoryImpl(tcpChannel)

    @Provides
    @Singleton
    fun provideCommandRepository(
        tcpChannel: TcpChannel,
        connectRepository: ConnectRepository,
        btManager: BluetoothConnectionManager,
        gson: Gson,
        commandRequestDao: CommandRequestDao,
        logRepository: LogRepository,
        ipcDeviceDao: IpcDeviceDao
    ): CommandRepository =
        CommandRepositoryImpl(tcpChannel, connectRepository, btManager, gson, commandRequestDao, logRepository, ipcDeviceDao)

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        //.baseUrl("http://10.0.2.2:8080/api/")
        .baseUrl("https://k14s207.p.ssafy.io/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}
