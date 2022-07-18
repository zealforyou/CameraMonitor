package cn.zz.cameraapp

import android.graphics.Bitmap

/**
 * <p>Created by ZhangZhuo on 2022/7/11.</p>
 */
data class MoveResult(val success: Boolean, val bmp: Bitmap?, val diff: Double)
