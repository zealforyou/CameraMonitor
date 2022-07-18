package cn.zz.cameraapp

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.zz.cameraapp.databinding.ActivitySplashBinding
import cn.zz.cameraapp.usb.UsbStorage
import cn.zz.cameraapp.utils.ToastUtil
import com.jiangdg.usbcamera.UVCCameraHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.io.File

class SplashActivity : AppCompatActivity() {
    private val mMissPermissions: MutableList<String> = ArrayList()

    private val isVersionM: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private val usbPmChannel = Channel<UsbDevice?>()
    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    lifecycleScope.launch {
                        if (device != null) {
                            usbPmChannel.send(device)
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.i(TAG, "USB device attached")
                if (device != null) {
//                    discoverDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.i(TAG, "USB device detached")

                if (device != null) {
//                    if (currentDevice != -1) {
//                        massStorageDevices[currentDevice].close()
//                    }
//                    // check if there are other devices or set action bar title
//                    // to no device if not
//                    discoverDevice()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(binding.root)
        if (isVersionM) {
            checkAndRequestPermissions()
        } else {
            startMainActivity()
        }
        //监听otg插入 拔出
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, usbDeviceStateFilter)
        //注册监听自定义广播
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    private fun checkAndRequestPermissions() {
        mMissPermissions.clear()
        for (permission in REQUIRED_PERMISSION_LIST) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission)
            }
        }
        if (mMissPermissions.isEmpty()) {
            startMainActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                mMissPermissions.toTypedArray(),
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mMissPermissions.remove(permissions[i])
                }
            }
        }
        if (mMissPermissions.isEmpty()) {
            startMainActivity()
        } else {
            Toast.makeText(this@SplashActivity, "get permissions failed,exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startMainActivity() {
        val file = File(UVCCameraHelper.ROOT_PATH + BaseApp.DIRECTORY_NAME)
        if (!file.exists()) {
            file.mkdirs()
        }
        initUsbStorage()
    }

    private fun initUsbStorage() {
        lifecycleScope.launch {
            binding.tvStatus.text = "初始化u盘"
            val devices = UsbMassStorageDevice.getMassStorageDevices(this@SplashActivity)
            for (device in devices) {
                if (requestUsbPm(device.usbDevice)) {
                    kotlin.runCatching {
                        device.init()
                        true
                    }.getOrNull() ?: continue
                    val currentFs = device.partitions.getOrNull(0)?.fileSystem ?: continue
                    Log.i(TAG, "Capacity: " + currentFs.capacity / 1024 / 1024 / 1024)
                    Log.i(TAG, "Free Space: " + currentFs.freeSpace / 1024 / 1024 / 1024)
                    UsbStorage.usbMass = device
                    ToastUtil.toast("找到u盘（总：${currentFs.capacity / 1024 / 1024 / 1024}G  剩余：${currentFs.freeSpace / 1024 / 1024 / 1024}G）")
                    break
                }
            }
            if ( UsbStorage.usbMass==null) {
                ToastUtil.toast("未能找到u盘")
            }
            binding.tvStatus.text = "初始化完成，准备录像"
            Handler().postDelayed({
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }, 1000)
        }
    }

    private suspend fun requestUsbPm(device: UsbDevice): Boolean {
        val usbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        if (usbManager.hasPermission(device)) return true
        usbManager.requestPermission(device, permissionIntent)
        binding.tvStatus.text = "初始化u盘：获取usb设备权限"
        val grantDevice = withTimeout(5000) {
            usbPmChannel.receive()
        }
        return device.deviceName == grantDevice?.deviceName
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "cn.zz.cameraapp.USB_PERMISSION"
        private val TAG = "SplashActivity"
        private val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_WIFI_STATE,
        )
        private const val REQUEST_CODE = 1
    }
}