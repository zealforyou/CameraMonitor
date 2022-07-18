package cn.zz.cameraapp.usb

import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File

/**
 * <p>Created by ZhangZhuo on 2022/6/1.</p>
 */
object UsbUtils {

    /**
     * 05a3是目前发出去的ai摄像头
     * 根据厂商id查找设备
     */
    @WorkerThread
    fun getDevBus(vendor: String = "05a3"): List<Int> {
        val process = Runtime.getRuntime().exec("lsusb")
        val input = process.inputStream.bufferedReader()
        var line: String? = null
        val list = ArrayList<Int>()
        while (kotlin.run {
                line = input.readLine()
                line
            } != null) {
            if (line?.contains(vendor) == true) {
                Regex("Bus (.+) Device").find(line!!)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                    list.add(it)
                }
            }
        }
        process.waitFor()
        return list
    }

    /**
     * 查找摄像头usb驱动所在的设备
     */
    @WorkerThread
    fun getDevBusByDriver(): List<Int> {
        //系统自带uvc驱动，但是app内的uvc库可能会把它卸载了
//        val uvcDriver = File("sys/bus/usb/drivers/uvcvideo")
//        val uvcList = uvcDriver.list { _, name ->
//            Regex("^\\d").containsMatchIn(name)
//        }?.map {
//            it.substring(0, 1).toIntOrNull()
//        }?.filterNotNull()?.distinct() ?: emptyList()
        //usb基础驱动
        val usbDriver = File("sys/bus/usb/drivers/usb")
        val usbList = HashSet<Int>()
        var hubBus = -1
        var hubHasDriver = false
        usbDriver.listFiles()?.forEach { parent ->
            if (!parent.isDirectory) return@forEach
            val result = Regex("^(\\d)-\\d\\.?(\\d?)").find(parent.name) ?: return@forEach
            val bus = result.groupValues.getOrNull(1)?.toIntOrNull()//总线号
            val hubNum = result.groupValues.getOrNull(2)?.toIntOrNull()//扩展口编号
            if (bus != null && hubNum != null) {
                //有扩展口
                hubBus = bus
                if (hubNum <= 3 && isVideoDriver(parent)) {//3个扩展口
                    hubHasDriver = true
                }
            } else if (bus != null && isVideoDriver(parent)) {
                usbList.add(bus)
            }
        }
        if (hubBus != -1) {
            if (hubHasDriver) {
                usbList.add(hubBus)
            } else {
                usbList.remove(hubBus)
            }
        }
        Log.i("usbUtils", "getDevBusByDriver : $usbList")
        return usbList.toList()
    }

    private fun isVideoDriver(parent: File) :Boolean{
        var drcount = 0
        //排除掉音频设备等
        parent.listFiles()?.forEach drcount@{ child ->
            if (!child.isDirectory) return@drcount
            if (drcount > 2) return false
            if (Regex("^(\\d)-\\d\\.?(\\d?):").find(child.name) != null) {
                drcount++
            }
        }
        return drcount != 1
    }

    /**
     * 返回usb编号（devName: xxx/usb/005/xxx）
     */
    fun getDevBusByName(devName: String): Int? {
        return Regex("usb[/\\\\](\\d+)").find(devName)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    @WorkerThread
    fun getAllSerial(): List<UsbSerial> {
        val driverBus = getDevBusByDriver()
        val list = HashMap<String, UsbSerial>()
        File("sys/bus/usb/devices").listFiles { _, name ->
            name.contains("usb")
        }?.forEach {
            val serail = kotlin.runCatching {
                File(it, "serial").readLines().getOrNull(0)
            }.getOrNull()
            val devBus = Regex("usb(.+)").find(it.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (serail != null && devBus != null) {
                if (list.containsKey(serail)) {
                    if (driverBus.contains(devBus)) {
                        list[serail] = UsbSerial(devBus, serail)
                    }
                } else {
                    list[serail] = UsbSerial(devBus, serail)
                }
            }
        }
        return list.values.sortedBy { it.devBus }
    }

    @WorkerThread
    fun getSerial(devBus: Int): String? {
        return kotlin.runCatching {
            File("sys/bus/usb/devices/usb${devBus}/serial").readLines().getOrNull(0)
        }.getOrNull()
    }

}