package com.example.sdkinstatest

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.arashivision.sdkcamera.InstaCameraSDK
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener
import com.arashivision.sdkmedia.export.ExportImageParamsBuilder
import com.arashivision.sdkmedia.export.ExportUtils
import com.arashivision.sdkmedia.export.ExportVideoParamsBuilder
import com.arashivision.sdkmedia.export.IExportCallback
import com.arashivision.sdkmedia.player.listener.PlayerGestureListener
import com.arashivision.sdkmedia.player.listener.PlayerViewListener
import com.arashivision.sdkmedia.player.listener.VideoStatusListener
import com.arashivision.sdkmedia.player.video.VideoParamsBuilder
import com.arashivision.sdkmedia.work.WorkWrapper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.sdkinstatest.camera_api.viewModel
import com.example.sdkinstatest.databinding.ActivityMainBinding
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.FileCallback
import com.lzy.okgo.model.Response
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : BaseOCameraActivity(R.layout.activity_main), OnClickListener,
    ICaptureStatusListener {
    private lateinit var binding: ActivityMainBinding
    lateinit var cameraBindNetworkManager: CameraBindNetworkManager
    lateinit var filePaths: Array<out String>
    lateinit var workWrapper: WorkWrapper
    var exportedImgUri = ""
    var mlist = mutableListOf<Bitmap>()
    val vM: viewModel by viewModels<viewModel>()
    var c = 0
    lateinit var camInst: InstaCameraManager

    companion object {
        val ExpPath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/SDK_DEMO_EXPORT/"
    }
    var fullPathVid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // do initializing for cameraSdk
        InstaCameraSDK.init(application)
        cameraBindNetworkManager = CameraBindNetworkManager()
        handleClicks()
        filePaths = emptyArray()

        // Capture Status Callback
        InstaCameraManager.getInstance().setCaptureStatusListener(this)

    }

    override fun onStart() {
        super.onStart()
        if (checkCameraConnection())
            utilites.showToastMsg("connected", this)
        else {
            cameraBindNetworkManager.instance?.bindNetwork(object :
                CameraBindNetworkManager.IBindNetWorkCallback {
                override fun onResult(errorCode: CameraBindNetworkManager.ErrorCode?) {
                    InstaCameraManager.getInstance()
                        .openCamera(InstaCameraManager.CONNECT_TYPE_WIFI)
                }
            })

        }
    }

    private fun checkCameraConnection(): Boolean =
        InstaCameraManager.getInstance().cameraConnectedType != InstaCameraManager.CONNECT_TYPE_NONE

    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
        if (enabled)
            utilites.showToastMsg("camera connected", applicationContext)
    }

    override fun initEvents() {
    }

    private fun handleClicks() {
        binding.captureBtn.setOnClickListener(this)
        binding.extractCapturedImg.setOnClickListener(this)
        binding.recordBtn.setOnClickListener(this)
        binding.stopRecordBtn.setOnClickListener(this)
    }

    override fun onClick(pView: View?) {
        when (pView) {
            binding.captureBtn -> {
                //vM.fetchingData()
                if (checkCameraConnection())
                    InstaCameraManager.getInstance().startNormalCapture(false)
                InstaCameraManager.getInstance().setCaptureStatusListener(this)
                onCameraSDCardStateChanged(InstaCameraManager.getInstance().isSdCardEnabled);
            }
            binding.extractCapturedImg -> {
                if (this.filePaths.isNotEmpty()) {
                    doExtractingImg(filePaths)
                }
            }
            binding.recordBtn -> {
                if (checkSdCardEnabled()) {
                    InstaCameraManager.getInstance().setCaptureStatusListener(this)
                    InstaCameraManager.getInstance().startNormalRecord()
                }
            }
            binding.stopRecordBtn -> {
                when (binding.stopRecordBtn.text) {
                    "Stop Record" -> {
                        InstaCameraManager.getInstance().stopNormalRecord()
                        // binding.stopRecordBtn.text ="Export Vid"
                    }
//                    "Export Vid"->{
//                        if (filePaths.isNotEmpty()) downloadRecordedFile(filePaths)
//                    }
                }
            }
        }
    }

    private fun downloadRecFile(urls: Array<out String>, filePaths: String) {
        val dialog: MaterialDialog = MaterialDialog.Builder(this)
            .title("Downloading Recorded Video")
            .content("Downloading...")
            .cancelable(false)
            .build()

        workWrapper = WorkWrapper(filePaths)
        val exportVidBuilder: ExportVideoParamsBuilder = ExportVideoParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.PANORAMA)
            .setTargetPath(fullPathVid)

        this.exportedImgUri = fullPathVid

        Log.d("?????", "exportedVid : $exportedImgUri")

        dialog.show()

        ExportUtils.exportVideo(workWrapper, exportVidBuilder,
            object : IExportCallback {
                override fun onSuccess() {
                    utilites.showToastMsg("url : $exportedImgUri", this@MainActivity)

                    dialog.dismiss()
                    lifecycleScope.launch {
                        playVideo(workWrapper)
                    }
                }

                override fun onFail(p0: Int, p1: String?) {
                    dialog.setContent("some error occurred while downloading")
                    dialog.setCancelable(true)
                }

                override fun onCancel() {
                    dialog.dismiss()
                }

                override fun onProgress(progress: Float) {
                    dialog.setContent("progress : ${(progress * 100).roundToInt()}%")
                }
            })
    }

    private fun playVideo(mWorkWrapper: WorkWrapper) {
        binding.playerVideo.visibility = View.VISIBLE
        binding.playerVideo.setLifecycle(lifecycle)
        binding.playerVideo.setPlayerViewListener(object : PlayerViewListener {
            override fun onLoadingStatusChanged(isLoading: Boolean) {}
            override fun onLoadingFinish() {
                Toast.makeText(
                    this@MainActivity,
                    "finished",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onFail(errorCode: Int, errorMsg: String) {
                // if GPU not support, errorCode is -10003 or -10005 or -13020
                val toast = "failed"
                Toast.makeText(this@MainActivity, toast, Toast.LENGTH_LONG).show()
            }
        })
        binding.playerVideo.setVideoStatusListener(object : VideoStatusListener {
            override fun onProgressChanged(position: Long, length: Long) {
                // mSeekBar.setMax(length.toInt())
                // mSeekBar.setProgress(position.toInt())
                // mTvCurrent.setText(TimeFormat.durationFormat(position))
                // mTvTotal.setText(TimeFormat.durationFormat(length))
            }

            override fun onPlayStateChanged(isPlaying: Boolean) {}
            override fun onSeekComplete() {
                binding.playerVideo.resume()
            }

            override fun onCompletion() {}
        })
        binding.playerVideo.setGestureListener(object : PlayerGestureListener {
            override fun onTap(e: MotionEvent): Boolean {
                if (binding.playerVideo.isPlaying()) {
                    binding.playerVideo.pause()
                } else if (!binding.playerVideo.isLoading && !binding.playerVideo.isSeeking) {
                    binding.playerVideo.resume()
                }
                return false
            }
        })
        val builder = VideoParamsBuilder()
        builder.isWithSwitchingAnimation = true
//        if (isPlaneMode) {
//            builder.renderModelType = VideoParamsBuilder.RENDER_MODE_PLANE_STITCH
//            builder.setScreenRatio(2, 1)
//        }
        binding.playerVideo.prepare(mWorkWrapper, builder)
        binding.playerVideo.play()
    }

    private fun downloadRecordedFile(urls: Array<out String>) {
        if (urls == null || urls.isEmpty()) return
        val localFolder =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}/SDK_EXPORT_RECORD"
        val fileName = mutableListOf<String>()
        val localPaths = mutableListOf<String>()
        var needDownload = false
        for (i in urls.indices) {
            fileName.add(urls[i].substring(urls[i].lastIndexOf("/") + 1))
            localPaths.add(localFolder + "/" + fileName[i])
            if (File(localPaths[i]).exists())
                needDownload = true
        }
        val dialog: MaterialDialog = MaterialDialog.Builder(this)
            .title("Downloading Recorded Video")
            .content("Downloading...")
            .cancelable(false)
            .build()

        dialog.show()

        var successfulCount: Int = 0

        var errorCount: Int = 0
        for (x in localPaths.indices) {
            val url = urls[x]
            OkGo.get<File>(url)
                .execute(object : FileCallback() {
                    override fun onSuccess(response: Response<File>?) {
                        errorCount++
                        checkDownload()
                    }

                    override fun onError(response: Response<File>?) {
                        successfulCount++
                        val rep = response.let {
                            it?.body()?.absoluteFile?.absolutePath
                        }
                        utilites.showToastMsg("path : $rep", this@MainActivity)
                        checkDownload()
                    }

                    private fun checkDownload() {
                        if (successfulCount + errorCount >= url.length)
                            dialog.dismiss()
                    }
                })
        }
    }

    private fun doExtractingImg(filePaths: Array<out String>) {
        val fullPathImg = "$ExpPath${System.currentTimeMillis()}.jpg"
        workWrapper = WorkWrapper(filePaths)
        val exportImgBuilder: ExportImageParamsBuilder = ExportImageParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.PANORAMA)
            .setTargetPath(fullPathImg)
        this.exportedImgUri = fullPathImg

        ExportUtils.exportImage(workWrapper, exportImgBuilder,
            object : IExportCallback {
                override fun onSuccess() {
                    utilites.showToastMsg("exported successfully", applicationContext)
                    Glide.with(binding.root)
                        .load(exportedImgUri)
                        .into(binding.exportedImg)
                    binding.exportedImg.visibility = VISIBLE

                    Glide.with(applicationContext)
                        .asBitmap()
                        .load(File(exportedImgUri))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                mlist.add(resource)
                                Log.d("?????", "storedBitmap Size : ${mlist.size}")
                                //we can do what we want ... like store converting bitmap to string and store it on room db...
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                TODO("Not yet implemented")
                            }
                        })
                }

                override fun onFail(p0: Int, p1: String?) {
                    utilites.showToastMsg("exported failed : $p1", applicationContext)
                }

                override fun onCancel() {
                    TODO("Not yet implemented")
                }
            })
    }

    override fun onCaptureFinish(filePathsM: Array<out String>?) {
        if (filePathsM != null && filePathsM.isNotEmpty()) {
            this.filePaths = filePathsM
            Log.d("??????", "filePathsM Size:${filePaths.size ?: -1} ")

            //downloadRecordedFile(filePathsM)
            workWrapper = WorkWrapper(filePaths)
            if (workWrapper.isVideo or workWrapper.isHDRVideo) {
                for (i in filePaths.indices) {
                    Log.d("????", "extensionFile f: ${filePaths[i]}")
                    c++
                    fullPathVid = "$ExpPath${System.currentTimeMillis()}m$c.mp4"
                    downloadRecFile(filePaths, filePaths[i])
                }
            } else {
                binding.exportedImg.visibility = View.VISIBLE
                doExtractingImg(filePaths)
            }
        }
    }

    override fun onCaptureStopping() {
        Log.d("??????", "filePathsM CaptureStopped: ")
    }

    override fun onCaptureTimeChanged(captureTime: Long) {
        Log.d("??????", "filePathsM time:${captureTime} ")
    }

    override fun onCameraSDCardStateChanged(enabled: Boolean) {
        if (checkCameraConnection() && !enabled)
            utilites.showToastMsg("SD card Not Provided or No enough space", this)
    }

    private fun checkSdCardEnabled(): Boolean {
        if (checkCameraConnection() && !InstaCameraManager.getInstance().isSdCardEnabled) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}