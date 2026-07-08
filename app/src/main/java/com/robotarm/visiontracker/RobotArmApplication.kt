package com.robotarm.visiontracker

import android.app.Application

class RobotArmApplication : Application() {
    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
        super.onCreate()
    }
}
