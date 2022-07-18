package cn.zz.cameraapp.face;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * <p>Created by ZhangZhuo on 2021/10/28.</p>
 */
public class FaceResult {
    private Mat detected;
    private Rect pos;

    public FaceResult(Mat faceMat, Rect pos) {
        this.detected = faceMat;
        this.pos = pos;
    }

    public Mat getDetected() {
        return detected;
    }

    public void setDetected(Mat detected) {
        this.detected = detected;
    }

    public Rect getPos() {
        return pos;
    }

    public void setPos(Rect pos) {
        this.pos = pos;
    }
}
