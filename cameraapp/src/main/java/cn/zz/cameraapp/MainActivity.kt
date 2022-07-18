package cn.zz.cameraapp

import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.zz.cameraapp.Config.*
import cn.zz.cameraapp.databinding.ActivityMainBinding
import cn.zz.cameraapp.ftp.FtpServerlet
import cn.zz.cameraapp.usb.UsbStorage
import cn.zz.cameraapp.usb.UsbUtils
import cn.zz.cameraapp.utils.AndroidStorageUtil
import cn.zz.cameraapp.utils.DebounceUtil
import cn.zz.cameraapp.utils.ToastUtil
import cn.zz.cameraapp.utils.WifiUtil
import com.jiangdg.usbcamera.UVCCameraHelper
import com.jiangdg.usbcamera.UVCCameraHelper.OnMyDevConnectListener
import com.jiangdg.usbcamera.utils.FileUtils
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.AbstractUVCCameraHandler
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener
import com.serenegiant.usb.encoder.RecordParams
import com.serenegiant.usb.encoder.biz.FpsUtils
import com.serenegiant.usb.widget.CameraViewInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), CameraViewInterface.Callback {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var mCameraHelper: UVCCameraHelper? = null
    private var busList: ArrayList<Int>? = null
    private var isPreview = false
    private var openCVIsSuccess = false
    private var moveCheck: MoveCheck? = null
    private var lastMoveTime = 0L
    private var ftpServer: FtpServerlet? = null

    @Volatile
    private var isRecording = false

    private val listener: OnMyDevConnectListener = object : OnMyDevConnectListener {
        override fun onAttachDev(device: UsbDevice) {
            if (busList == null) {
                busList = ArrayList()
                busList?.addAll(UsbUtils.getDevBusByDriver())
            }
            if (busList?.contains(UsbUtils.getDevBusByName(device.deviceName)) == true) {
                ToastUtil.toast("找到相机设备")
                Log.i("www", "找到相机设备")
            }
            mCameraHelper?.requestPermission(device)
        }

        override fun onDettachDev(device: UsbDevice) {
        }

        override fun onConnectDev(device: UsbDevice, isConnected: Boolean) {
        }

        override fun onDisConnectDev(device: UsbDevice) {
        }
    }

    private val callback: AbstractUVCCameraHandler.CameraCallback = object : AbstractUVCCameraHandler.SimpleCameraCallback() {
        override fun onOpen(ctrlBlock: USBMonitor.UsbControlBlock?) {
            isPreview = ctrlBlock != null
            ToastUtil.toast("摄像头打开成功")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        StorageSizeMonitor.init()
        binding.cameraView.setCallback(this)
        mCameraHelper = UVCCameraHelper.getInstance(BaseApp.CAMERA_WIDTH, BaseApp.CAMERA_HEIGHT)
        mCameraHelper?.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        mCameraHelper?.initUSBMonitor(this, binding.cameraView, listener)
        mCameraHelper?.addCallback(callback)
        mCameraHelper?.setOnPreviewFrameListener(object : AbstractUVCCameraHandler.OnPreViewResultListener {
            var isCheck = false
            var firstFrame = false
            override fun onPreviewResult(nv21Yuv: ByteArray) {
                if (!firstFrame) {
                    firstFrame = true
                    ToastUtil.toast("收到首帧数据")
                }
                if (!isCheck) {
                    isCheck = true
                    lifecycleScope.launch(Dispatchers.IO) {
                        DebounceUtil.debounce(500) {
                            val result = kotlin.runCatching { moveCheck?.check(nv21Yuv) }.onFailure {
                                it.printStackTrace()
                            }.getOrNull()
                            launch(Dispatchers.Main) {
                                onMoveResult(result)
                            }
                        }
                        isCheck = false
                    }
                }
            }
        })
        initOpencv()
        mCameraHelper?.registerUSB()
        lifecycleScope.launch(Dispatchers.IO) {
            ftpServer = FtpServerlet(BaseApp.getVideoDir()) { tag, content ->
                Log.i(tag, content ?: return@FtpServerlet)
            }
            ftpServer?.startFtp()
        }
        initView()
    }

    private fun initView() {
        lifecycleScope.launch {
            while (isActive) {
                binding.fps.text = "fps:${FpsUtils.getSamplingFps()}"
                delay(1000)
            }
        }
        binding.ftpIp.text = "FTP:${WifiUtil.getLocalIp()}:2121/"
        if (UsbStorage.rootFile == null) {
            binding.storageSize.text = "机身剩余空间：${AndroidStorageUtil.getSDAvailableSize()}"
        } else {
            binding.storageSize.text = "U盘剩余空间：${UsbStorage.getAvailableSize()}"
        }
    }

    private fun initOpencv() {
        val mLoaderCallback = object : LoaderCallbackInterface {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        loopCheckMove()
                        openCVIsSuccess = true
                        moveCheck = MoveCheck()
                        moveCheck?.init()
                    }
                }
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
            }
        }
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
        if (!isPreview && mCameraHelper?.isCameraOpened == true) {
            mCameraHelper?.startPreview(binding.cameraView)
            isPreview = true
        }
    }

    override fun onSurfaceChanged(view: CameraViewInterface?, surface: Surface?, width: Int, height: Int) {
    }

    override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
        if (isPreview && mCameraHelper!!.isCameraOpened) {
            mCameraHelper!!.stopPreview()
            isPreview = false
        }
    }

    @MainThread
    private fun onMoveResult(result: MoveResult?) {
        result?.bmp?.let {
            binding.image.setImageBitmap(it)
        }
        result?.diff?.let {
            binding.tvMoveDiff.text = "${String.format("Diff：%.2f", it)}"
        }
        if (result?.success == true) {
            lastMoveTime = System.currentTimeMillis()
            if (mCameraHelper?.isPushing?.not() == true && !isRecording) {
                isRecording = true
                startRecord()
            }
        }
    }

    private fun loopCheckMove() {
        lifecycleScope.launch {
            while (isActive) {
                if (lastMoveTime != 0L && System.currentTimeMillis() - lastMoveTime > CheckMoveTime) {
                    lastMoveTime = 0
                    stopRecord()
                }
                delay(1000)
            }
        }
    }

    private fun stopRecord() {
        if (mCameraHelper?.isPushing == true && isRecording) {
            binding.tvRecordStatus.text = "未开始录制"
            binding.tvRecordStatus.setTextColor(Color.RED)
            isRecording = false
            FileUtils.releaseFile()
            mCameraHelper?.stopPusher()
            Log.i("www", "stop record...")
            ToastUtil.toast("stop record...")
        }
    }

    private fun startRecord() {
        if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) {
            Toast.makeText(this, "sorry,camera open failed", Toast.LENGTH_SHORT).show()
            return
        }
        if (mCameraHelper?.isPushing?.not() == true) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").format(Date())
            val videoPath = BaseApp.getVideoDir() + dateStr

            val params = RecordParams()
            params.recordPath = videoPath
            params.recordDuration = recordDuration // auto divide saved,default 0 means not divided
            params.isVoiceClose = isVoiceClose // is close voice
            params.isSupportOverlay = true // overlay only support armeabi-v7a & arm64-v8a
            mCameraHelper?.startPusher(params, object : OnEncodeResultListener {
                override fun onEncodeResult(data: ByteArray, offset: Int, length: Int, timestamp: Long, type: Int) {
                    // type = 1,h264 video stream
                    if (type == 1) {
                        FileUtils.putFileStream(data, offset, length)
                    }
                    // type = 0,aac audio stream
                    if (type == 0) {

                    }
                }

                override fun onRecordResult(tempFilePath: String) {
                    if (TextUtils.isEmpty(tempFilePath)) {
                        return
                    }
                    thread {
                        val finalVideoFile = File(tempFilePath.substring(0, tempFilePath.indexOf("-tmp")))
                        kotlin.runCatching { Thread.sleep(100) }
                        File(tempFilePath).renameTo(finalVideoFile)
                        kotlin.runCatching { Thread.sleep(100) }
                        Log.i("www", "save videoPath:$finalVideoFile")
                        Handler(mainLooper).post {
                            ToastUtil.toast("save videoPath:$finalVideoFile")
                        }
                        UsbStorage.copyFrom(finalVideoFile)
                    }
                }
            })
            binding.tvRecordStatus.text = "录制中"
            binding.tvRecordStatus.setTextColor(Color.GREEN)
            ToastUtil.toast("start record...")
            Log.i("www", "start record...")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
        StorageSizeMonitor.destroy()
        mCameraHelper?.unregisterUSB()
        mCameraHelper?.release()
        ftpServer?.stopFtp()
        ftpServer?.destroy()
        ftpServer = null
    }
}