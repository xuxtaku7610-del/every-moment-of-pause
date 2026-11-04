package com.emop.pause

import android.app.Application
import com.emop.pause.service.TripServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PauseApp : Application() {
    /** 여행 중 포그라운드 서비스 (v10-1 3번). */
    @Inject lateinit var tripService: TripServiceController

    override fun onCreate() {
        super.onCreate()
        tripService.attach(this)
    }
}
