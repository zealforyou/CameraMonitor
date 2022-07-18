package cn.zz.cameraapp

import android.app.Application
import android.content.Context
import com.jiangdg.usbcamera.UVCCameraHelper
import java.io.File

/**
 * <p>Created by ZhangZhuo on 2022/7/11.</p>
 */
class BaseApp : Application() {
    companion object {
        @JvmStatic
        lateinit var app: BaseApp

        @JvmStatic
        val DIRECTORY_NAME = "CameraApp"

        @JvmStatic
        val CAMERA_WIDTH = 1280

        @JvmStatic
        val CAMERA_HEIGHT = 720
        @JvmStatic
        fun getVideoDir(): String {
            return (UVCCameraHelper.ROOT_PATH + BaseApp.DIRECTORY_NAME + "/videos/")
        }
    }

    private var mCrashHandler: CrashHandler? = null
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
    }


    override fun onCreate() {
        super.onCreate()
//        mCrashHandler = CrashHandler.getInstance()
//        mCrashHandler?.init(applicationContext, javaClass)
    }
}