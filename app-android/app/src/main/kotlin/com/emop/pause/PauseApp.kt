package com.emop.pause

import android.app.Application
import com.emop.pause.core.restriction.DriveStateRepository
import com.emop.pause.core.restriction.StubDriveStateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@HiltAndroidApp
class PauseApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // TODO(M1): CarUxRestrictions 구현으로 교체.
    @Provides
    @Singleton
    fun driveStateRepository(): DriveStateRepository = StubDriveStateRepository()
}
