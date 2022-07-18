package com.serenegiant.usb.encoder.biz

import java.util.concurrent.CopyOnWriteArrayList

/**
 * <p>Created by ZhangZhuo on 2022/1/19.</p>
 */
object FpsUtils {
    private var fpsTime = 0L
    private var lastFps = 0
    private var currentFps = 0
    private var fpsList = CopyOnWriteArrayList<Int>()

    @Synchronized
    fun compute() {
        if (fpsTime == 0L) {
            fpsTime = System.currentTimeMillis()
            currentFps++
            return
        }
        if (System.currentTimeMillis() - fpsTime >= 1000) {
            fpsTime = System.currentTimeMillis()
            lastFps = currentFps
            currentFps = 0
            if (fpsList.size > 30) {
                fpsList.removeFirstOrNull()
            }
            fpsList.add(lastFps)
        } else {
            currentFps++
        }
    }

    /**
     * 采样
     */
    @Synchronized
    fun getSamplingFps(): Int {
        return if (System.currentTimeMillis() - fpsTime > 2000) {
            0
        } else {
            lastFps
        }
    }

    /**
     * 平均 (目前最大30秒钟)
     * @param rangeTimeS 多少秒内
     */
    @Synchronized
    fun getAverageFps(rangeTimeS: Int): Int {
        return fpsList.takeLast(rangeTimeS).average().toInt()
    }

    /**
     * 方差
     */
    @Synchronized
    fun getVariance(rangeTimeS: Int): Int {
        val arg = getAverageFps(rangeTimeS)
        var sum = 0
        var count = 0
        for (i in 0 until rangeTimeS) {
            val fps = fpsList.getOrNull(fpsList.size - 1 - i)
            if (fps != null) {
                sum += (fps - arg) * (fps - arg)
                count++
            } else {
                break
            }
        }
        return if (count > 0) sum / count else 0
    }

    @Synchronized
    fun getMaxFps(rangeTimeS: Int): Int {
        return fpsList.takeLast(rangeTimeS).maxOrNull() ?: 0
    }
    @Synchronized
    fun getMinFps(rangeTimeS: Int): Int {
        return fpsList.takeLast(rangeTimeS).minOrNull() ?: 0
    }
    @Synchronized
    fun getUpdateTimeMs(): Long {
        return fpsTime
    }
}