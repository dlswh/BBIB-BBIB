package com.s14p31s207.android.feature.control.di

import com.s14p31s207.android.data.repository.DeviceConnectionRepositoryImpl
import com.s14p31s207.android.domain.repository.DeviceConnectionRepository
import com.s14p31s207.android.feature.control.error.ControlErrorManager
import com.s14p31s207.android.feature.control.error.DefaultControlErrorManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ControlModule {

    @Binds
    @Singleton
    abstract fun bindControlErrorManager(
        defaultControlErrorManager: DefaultControlErrorManager,
    ): ControlErrorManager

    // ControlModule.kt 에 추가

    @Binds
    @Singleton
    abstract fun bindDeviceConnectionRepository(
        impl: DeviceConnectionRepositoryImpl
    ): DeviceConnectionRepository
}
