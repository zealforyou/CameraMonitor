package cn.zz.cameraapp.usb

import android.text.format.Formatter
import android.util.Log
import cn.zz.cameraapp.BaseApp
import cn.zz.cameraapp.BaseApp.Companion.DIRECTORY_NAME
import cn.zz.cameraapp.utils.ToastUtil
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileOutputStream
import java.io.File
import java.util.concurrent.Executors

/**
 * <p>Created by ZhangZhuo on 2022/7/13.</p>
 */
object UsbStorage {
    private var es = Executors.newSingleThreadExecutor()
    var usbMass: UsbMassStorageDevice? = null
    var rootFile: UsbFile? = null
        get() {
            return usbMass?.partitions?.get(0)?.fileSystem?.rootDirectory
        }

    fun getAvailableSize(): String {
        return Formatter.formatFileSize(BaseApp.app, getAvailableSizeLong())
    }

    fun getAvailableSizeLong(): Long {
        return usbMass?.partitions?.get(0)?.fileSystem?.freeSpace ?: 0
    }

    fun getVideoDir(): UsbFile? {
        val root = rootFile ?: return null
        return kotlin.runCatching { root.search(DIRECTORY_NAME) }.getOrNull()
    }

    fun copyFrom(file: File) {
        if (!file.exists()) {
            Log.i("UsbStorage", "文件不存在：${file.absolutePath}")
            ToastUtil.toast("UsbStorage: 文件不存在：${file.absolutePath}")
            return
        }
        if (rootFile==null) {
            Log.i("UsbStorage", "u盘不存在")
            return
        }
        es.execute {
            ToastUtil.toast("UsbStorage: copy from ${file.absolutePath}")
            Log.i("UsbStorage", "UsbStorage copy from ${file.absolutePath}")
            val root = rootFile ?: return@execute
            var cameraVideo = kotlin.runCatching { root.search("${DIRECTORY_NAME}/${file.parent}") }.getOrNull()
            if (cameraVideo == null) {
                cameraVideo = kotlin.runCatching { root.createDirectory("${DIRECTORY_NAME}/${file.parent}") }.getOrNull() ?: return@execute
            }
            val videoFile = kotlin.runCatching { cameraVideo.createFile(file.name) }.getOrNull() ?: return@execute
            val buffer = ByteArray(1024 * 8)
            var len = 0
            val usbOs = UsbFileOutputStream(videoFile)
            kotlin.runCatching { file.inputStream() }.getOrNull()?.buffered()?.use {
                kotlin.runCatching {
                    while (kotlin.run { len = it.read(buffer);len } > 0) {
                        usbOs.write(buffer, 0, len)
                    }
                }.onSuccess {
                    kotlin.runCatching { file.delete() }
                }
            }
            if (!videoFile.isDirectory) {
                kotlin.runCatching {
                    usbOs.close()
                }
            }
        }
    }
}