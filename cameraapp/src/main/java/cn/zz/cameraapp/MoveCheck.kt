package cn.zz.cameraapp

import android.graphics.Bitmap
import android.util.Log
import cn.zz.cameraapp.face.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.video.BackgroundSubtractorMOG2
import org.opencv.video.Video

/**
 * <p>Created by ZhangZhuo on 2022/7/11.</p>
 */
class MoveCheck {
    private var mog2: BackgroundSubtractorMOG2? = null
    private var kernel: Mat? = null
    private var frameMat: Mat? = null
    private var mask: Mat? = null
    private var detector: FaceDetector? = null
    private var bmp: Bitmap? = null

    fun init() {
        mog2 = Video.createBackgroundSubtractorMOG2()
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        MainScope().launch {
            withContext(Dispatchers.IO) {
                val detector = FaceDetector()
                detector.init()
                this@MoveCheck.detector = detector
            }
        }
    }

    fun check(frameNv12: ByteArray): MoveResult? {
        if (mog2 == null || detector == null) {
            return null
        }
        val width = BaseApp.CAMERA_WIDTH
        val height = BaseApp.CAMERA_HEIGHT
        if (frameMat == null) {
            frameMat = Mat(height * 3 / 2, width, CvType.CV_8UC1)
            mask = Mat(height * 3 / 2, width, CvType.CV_8UC1)
            bmp = Bitmap.createBitmap(width, height * 3 / 2, Bitmap.Config.RGB_565)
        }
        var diff = 0.0
//        val result = detector?.containFaceNv21(frameNv12, width, height)
//        if (result?.pos != null) {
////            Utils.matToBitmap(result.detected, bmp)
//            Log.i("www", "检测到人:${result.pos}")
//        } else {
        frameMat?.put(0, 0, frameNv12)
        mog2?.apply(frameMat, mask)
        diff = Core.mean(mask).`val`[0]
        if (diffOfMove(diff)) {
            Log.i("www", "检测到画面变动:$diff")
        }
        if (!Config.isSimpleMode) {
            Utils.matToBitmap(mask, bmp)
        }

//        }
        return MoveResult(
//            result?.pos != null ||
            diffOfMove(diff), if (!Config.isSimpleMode) bmp else null, diff
        )
    }

    private fun diffOfMove(diff: Double): Boolean {
        val diffThreshold = 1
        return diff > diffThreshold
    }
}