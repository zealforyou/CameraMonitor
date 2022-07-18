
package cn.zz.cameraapp.face;

import android.content.Context;
import android.util.Log;

import androidx.annotation.WorkerThread;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.zz.cameraapp.BaseApp;

/**
 * Created by khq on 2021/06/15.
 */
public class FaceDetector {

    private static final String TAG = "FaceDetector";

    private Mat frame = null;
    private Mat rgb = null;
    private int absoluteFaceSize;
    private CascadeClassifier classifier;

    /**
     * 加载人脸检测模型
     */
    @WorkerThread
    public void init() {
        try {
            //OpenCV的人脸模型文件：
            InputStream is = BaseApp.app.getAssets().open("haarcascades/haarcascade_frontalcatface.xml");
//            InputStream is = BaseApp.app.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = BaseApp.app.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalcatface.xml");
            if (!mCascadeFile.exists()) {
                FileOutputStream os = new FileOutputStream(mCascadeFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
            // 加载cascadeClassifier
            classifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.i(TAG, "FaceDetector初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "FaceDetector初始化异常", e);
        }
    }

    public FaceResult containFaceI420(byte[] i420Data, int preViewW, int preViewH) {
        return containFace(Imgproc.COLOR_YUV2RGB_I420, i420Data, preViewW, preViewH);
    }

    public FaceResult containFaceNv21(byte[] nv21Yuv, int preViewW, int preViewH) {
        return containFace(Imgproc.COLOR_YUV2RGB_NV21, nv21Yuv, preViewW, preViewH);
    }

    /**
     * nv21数据里是否包含人脸
     *
     * @return
     */

    public FaceResult containFace(int code, byte[] data, int preViewW, int preViewH) {
        try {
            //1.初始化一个矩阵
            if (frame == null) {
                frame = new Mat(preViewH * 3 / 2, preViewW, CvType.CV_8UC1);
            }
            if (rgb == null) {
                rgb = new Mat();
            }
            frame.put(0, 0, data);
            //2.转换成bgr的mat
            Imgproc.cvtColor(frame, rgb, code);//转换颜色空间
            MatOfRect faces = new MatOfRect();

            int height = preViewH;

            if (absoluteFaceSize == 0) {
                if (Math.round(height * 0.01f) > 0) {
                    absoluteFaceSize = Math.round(height * 0.01f);
                }
            }
            classifier.detectMultiScale(rgb, faces, 1.2, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size(height, height));

            Rect[] facesArray = faces.toArray();
//            LogUtils.i(TAG, "人脸识别结果" + facesArray.length);
            return new FaceResult(rgb, facesArray.length >= 1 ? facesArray[0] : null);
        } catch (Exception e) {
            Log.i(TAG, "人脸识别异常" + e.getLocalizedMessage());
        }
        return null;
    }

    public void clear() {
        if (frame != null) {
            frame.release();
        }
        if (rgb != null) {
            rgb.release();
        }
    }
}
