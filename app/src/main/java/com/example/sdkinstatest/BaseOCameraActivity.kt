package com.example.sdkinstatest

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback
import com.example.sdkinstatest.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseOCameraActivity(layout:Int):AppCompatActivity(layout), ICameraChangedCallback{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InstaCameraManager.getInstance().registerCameraChangedCallback(this)
        initEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this)
    }

    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
    }

    override fun onCameraConnectError(errorCode: Int) {
        super.onCameraConnectError(errorCode)
    }

    override fun onCameraSDCardStateChanged(enabled: Boolean) {
        super.onCameraSDCardStateChanged(enabled)
    }

    override fun onCameraStorageChanged(freeSpace: Long, totalSpace: Long) {
        super.onCameraStorageChanged(freeSpace, totalSpace)
    }

    override fun onCameraBatteryLow() {
        super.onCameraBatteryLow()
    }

    override fun onCameraBatteryUpdate(batteryLevel: Int, isCharging: Boolean) {
        super.onCameraBatteryUpdate(batteryLevel, isCharging)
    }

    override fun onCameraSensorModeChanged(cameraSensorMode: Int) {
        super.onCameraSensorModeChanged(cameraSensorMode)
    }

    abstract fun initEvents()
}