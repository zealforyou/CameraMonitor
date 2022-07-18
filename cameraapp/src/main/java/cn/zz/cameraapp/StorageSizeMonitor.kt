package cn.zz.cameraapp

import android.util.Log
import cn.zz.cameraapp.Config.bit_rate
import cn.zz.cameraapp.Config.time_interval_s
import cn.zz.cameraapp.usb.UsbStorage
import cn.zz.cameraapp.utils.AndroidStorageUtil
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * <p>Created by ZhangZhuo on 2022/7/14.</p>
 */
object StorageSizeMonitor {
    private var es: ExecutorService? = null
    private var job: Job? = null
    private var availableSize = 0L
    fun init() {
        es = Executors.newSingleThreadExecutor()
        availableSize = getAvaSize()
        timer()
    }


    private fun timer() {
        job?.cancel()
        es?.asCoroutineDispatcher()?.let {
            job = CoroutineScope(SupervisorJob() + it).launch {
                while (isActive) {
                    checkFileNeedDel()
                    delay(time_interval_s * 1000L)
                }
            }
        }
    }

    private fun checkFileNeedDel() {
        availableSize = getAvaSize()
        if (availableSize < time_interval_s * bit_rate / 8L) {
            if (UsbStorage.rootFile == null) {
                deleteOldFile(time_interval_s * bit_rate / 8L)
            } else {
                deleteOldFileFromUsb(time_interval_s * bit_rate / 8L)
            }
        }
    }

    fun destroy() {
        job?.cancel()
        es?.shutdown()
    }

    private fun getAvaSize(): Long {
        return if (UsbStorage.rootFile == null) {
            AndroidStorageUtil.getSDAvailableSizeLong()
        } else {
            UsbStorage.getAvailableSizeLong()
        }
    }

    private fun deleteOldFile(limit: Long) {
        var childSize = 0L
        var count = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val sdf1 = SimpleDateFormat("HH:mm:ss")
        val root = BaseApp.getVideoDir()
        File(root).list()?.sortedBy {
            kotlin.runCatching { sdf.parse(it)?.time }.getOrNull() ?: Long.MAX_VALUE
        }?.forEach parent@{
            val childDir = File(root, it)
            if (childDir.exists().not() || childDir.isFile) {
                return@parent
            }
            childDir.list()?.sortedBy {
                kotlin.runCatching { sdf1.parse(it)?.time }.getOrNull() ?: Long.MAX_VALUE
            }?.forEach child@{
                val child = File(childDir, it)
                if (child.isDirectory) return@child
                if (childSize < limit) {
                    if (!it.contains("-tmp")) {
                        count++
                        childSize += child.length()
                        child.delete()
                    }
                } else {
                    Log.i("StorageSizeMonitor", "删除旧文件：${count}个，总大小 ${childSize / 1024 / 1024}M")
                    return
                }
            }
        }
        Log.i("StorageSizeMonitor", "删除旧文件：${count}个，总大小 ${childSize / 1024 / 1024}M")
    }

    private fun deleteOldFileFromUsb(limit: Long) {
        var childSize = 0L
        var count = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val sdf1 = SimpleDateFormat("HH:mm:ss")
        UsbStorage.getVideoDir()?.listFiles()?.sortedBy {
            kotlin.runCatching { sdf.parse(it.name)?.time }.getOrNull() ?: Long.MAX_VALUE
        }?.forEach parent@{ childDir ->
            if (!childDir.isDirectory) {
                return@parent
            }
            childDir.listFiles().sortedBy {
                kotlin.runCatching { sdf1.parse(it.name)?.time }.getOrNull() ?: Long.MAX_VALUE
            }.forEach child@{ child ->
                if (child.isDirectory) return@child
                if (childSize < limit) {
                    if (!child.name.contains("-tmp")) {
                        count++
                        childSize += child.length
                        child.delete()
                    }
                } else {
                    Log.i("StorageSizeMonitor", "u盘删除旧文件：${count}个，总大小 ${childSize / 1024 / 1024}M")
                    return
                }
            }
        }
        Log.i("StorageSizeMonitor", "u盘删除旧文件：${count}个，总大小 ${childSize / 1024 / 1024}M")
    }

    private fun deleteDirWihtFile(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        for (file in dir.listFiles()) {
            if (file.isFile) file.delete() // 删除所有文件
            else if (file.isDirectory) deleteDirWihtFile(file) // 递规的方式删除文件夹
        }
        dir.delete() // 删除目录本身
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        if (dir.isFile) return dir.length()
        var sum = 0L
        dir.listFiles()?.forEach {
            sum += getDirSize(it)
        }
        return sum
    }
}