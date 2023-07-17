package com.example.sdkinstatest

import android.app.Application
import com.arashivision.sdkcamera.InstaCameraSDK
import com.arashivision.sdkmedia.InstaMediaSDK
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp:Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        InstaCameraSDK.init(this)
        InstaMediaSDK.init(this)

    }

    companion object{
        var instance= MyApp()
    }
}