package com.example.sdkinstatest

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Process
import android.text.TextUtils

class CameraBindNetworkManager {

    enum class ErrorCode {
        OK, BIND_NETWORK_FAIL
    }

    private val ACTION_BIND_NETWORK_NOTIFY = "com.arashivision.sdk.demo.ACTION_BIND_NETWORK_NOTIFY"
    private val EXTRA_KEY_IS_BIND = "extra_key_is_bind"

    private var sInstance: CameraBindNetworkManager? = null

    private var mHasBindNetwork = false
    private var mIsBindingNetwork = false
    private var mProcessName: String? = null

    companion object{

    }

    val instance: CameraBindNetworkManager?
        get() {
            if (sInstance == null) {
                sInstance = CameraBindNetworkManager()
            }
            return sInstance
        }

    // 在Application检测如果当前是非连接相机进程且需要与相机通讯的话就调用此方法，否则不需要
    // In Application, if it is currently a non-connected camera process and needs to communicate with the camera, call this method, otherwise it is not needed
    fun initWithOtherProcess() {
        mProcessName = processName
        bindNetwork(null)
        registerChildProcessBindNetworkReceiver()
    }

    private fun registerChildProcessBindNetworkReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_BIND_NETWORK_NOTIFY)
        MyApp.instance.registerReceiver(mOtherProcessBindNetworkReceiver, intentFilter)
    }

    // 相机进程控制绑定/解绑的同时调用此方法对其它进程绑定/解绑
    // 如果没有在其它进程和相机通讯的，不需要此方法
    // The camera process controls the binding/unbinding while calling this method to bind/unbind other processes
    // If there is no communication with the camera in other processes, this method is not needed
    fun notifyOtherProcessBind(isBind: Boolean) {
        val intent = Intent()
        intent.action = ACTION_BIND_NETWORK_NOTIFY
        intent.putExtra(EXTRA_KEY_IS_BIND, isBind)
        MyApp.instance.sendBroadcast(intent)
    }

    // 在需要连接相机前绑定相机WIFI
    // Bind the camera WIFI before connecting the camera
    fun bindNetwork(bindNetWorkCallback: IBindNetWorkCallback?) {
        if (mIsBindingNetwork) {
            bindNetWorkCallback?.onResult(ErrorCode.OK)
        } else if (mHasBindNetwork) {
            bindNetWorkCallback?.onResult(ErrorCode.OK)
        } else {
            bindWifiNet(bindNetWorkCallback)
        }
    }

    // 需要断开相机前解绑WIFI，然后再调用closeCamera()
    // Need to unbind the WIFI before disconnecting the camera, and then call closeCamera()
    fun unbindNetwork() {
        if (mHasBindNetwork) {
            unbindWifiNet()
        }
        if (mIsBindingNetwork) {
            mIsBindingNetwork = false
        }
    }

    private fun bindWifiNet(bindNetWorkCallback: IBindNetWorkCallback?) {
        if (mIsBindingNetwork) {
            return
        }
        mIsBindingNetwork = true
        val network = wifiNetwork
        if (network != null) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    if (ConnectivityManager.setProcessDefaultNetwork(network)) {
                        mHasBindNetwork = true
                        mIsBindingNetwork = false
                        bindNetWorkCallback?.onResult(ErrorCode.OK)
                    } else {
                        mIsBindingNetwork = false
                        bindNetWorkCallback?.onResult(ErrorCode.BIND_NETWORK_FAIL)
                    }
                } else {
                    val connectivityManager = MyApp.instance
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    if (connectivityManager.bindProcessToNetwork(network)) {
                        mHasBindNetwork = true
                        mIsBindingNetwork = false
                        bindNetWorkCallback?.onResult(ErrorCode.OK)
                    } else {
                        mIsBindingNetwork = false
                        bindNetWorkCallback?.onResult(ErrorCode.BIND_NETWORK_FAIL)
                    }
                }
            } catch (e: IllegalStateException) {
                mIsBindingNetwork = false
                bindNetWorkCallback?.onResult(ErrorCode.BIND_NETWORK_FAIL)
            }
        } else {
            mIsBindingNetwork = false
            bindNetWorkCallback?.onResult(ErrorCode.BIND_NETWORK_FAIL)
        }
    }

    private val wifiNetwork: Network?
        private get() {
            val connManager = MyApp.instance
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (network in connManager.allNetworks) {
                val netInfo = connManager.getNetworkInfo(network)
                if (netInfo != null && netInfo.type == ConnectivityManager.TYPE_WIFI) {
                    return network
                }
            }
            return null
        }

    private fun unbindWifiNet() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (ConnectivityManager.setProcessDefaultNetwork(null)) {
                mHasBindNetwork = false
            }
        } else {
            val connectivityManager = MyApp.instance
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager.bindProcessToNetwork(null)) {
                mHasBindNetwork = false
            }
        }
    }

    private val mOtherProcessBindNetworkReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (TextUtils.equals(intent.action, ACTION_BIND_NETWORK_NOTIFY)) {
                val isBind = intent.getBooleanExtra(EXTRA_KEY_IS_BIND, false)
                if (isBind) {
                    bindNetwork(null)
                } else {
                    unbindNetwork()
                }
            }
        }
    }

    private val processName: String?
        private get() {
            val pid = Process.myPid()
            val manager =
                MyApp.instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var runningAppProcessesList = manager.runningAppProcesses
            if (runningAppProcessesList == null) {
                runningAppProcessesList = ArrayList()
            }
            for (process in runningAppProcessesList) {
                if (process.pid == pid) {
                    return process.processName
                }
            }
            return null
        }

    /************************* interface  */
    interface IBindNetWorkCallback {
        fun onResult(errorCode: ErrorCode?)
    }

}
