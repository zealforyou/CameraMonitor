package cn.zz.cameraapp.utils

/**
 * 过滤短时间内的重复动作
 * <p>Created by ZhangZhuo on 2022/6/2.</p>
 */
object DebounceUtil {
    private val bounceMap = HashMap<String, Long>()

    fun debounce(time: Int, action: Runnable) {
        debounce("default", time, action)
    }

    fun debounce(key: String, time: Int, action: Runnable) {
        if (filterWithTime(key, time)) {
            action.run()
        }
    }

    fun debounceWithBool(key: String, time: Int): Boolean {
        return filterWithTime(key, time)
    }

    private fun filterWithTime(key: String, time: Int): Boolean {
        var canRun = false
        synchronized(key) {
            val lastTime = bounceMap[key] ?: 0
            if (System.currentTimeMillis() - lastTime > time) {
                bounceMap[key] = System.currentTimeMillis()
                canRun = true
            }
        }
        return canRun
    }
}