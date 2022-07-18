package cn.zz.cameraapp.utils

import android.os.Looper
import android.widget.Toast
import cn.zz.cameraapp.BaseApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * <p>Created by ZhangZhuo on 2022/7/11.</p>
 */
object ToastUtil {
    @JvmStatic
    fun toast(text: String) {
        val isMain = kotlin.runCatching { Looper.myLooper() == Looper.getMainLooper() }.getOrElse { false }
        if (isMain) {
            Toast.makeText(BaseApp.app, text, Toast.LENGTH_SHORT).show()
        } else {
            MainScope().launch {
                Toast.makeText(BaseApp.app, text, Toast.LENGTH_SHORT).show()
            }
        }
    }
}