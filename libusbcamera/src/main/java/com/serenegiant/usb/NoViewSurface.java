package com.serenegiant.usb;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.view.Surface;

import com.serenegiant.usb.encoder.IVideoEncoder;
import com.serenegiant.usb.widget.CameraViewInterface;

/**
 * 没有界面的surface，用来在真正的考试页面用
 * Created by khq on 2020/07/06.
 */
public class NoViewSurface implements CameraViewInterface {

    private double ratio;

    private final int width;
    private final int height;

    private SurfaceTexture preSurface;
    private Callback callback;
    private Surface surface;

    private boolean isDestroyed = false;

    private final SurfaceTexture.OnFrameAvailableListener listener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (isDestroyed) {
                return;
            }
            //preSurface.updateTexImage();
        }
    };

    public NoViewSurface(int width, int height) {
        this.width = width;
        this.height = height;
        preSurface = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        preSurface.setDefaultBufferSize(width, height);
        preSurface.setOnFrameAvailableListener(listener);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    /**
     * 销毁
     */
    public void destroy() {
        isDestroyed = true;
        callback.onSurfaceDestroy(this, getSurface());
        preSurface.release();
        preSurface = null;
        surface = null;
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
        callback.onSurfaceCreated(this, getSurface());
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return preSurface;
    }

    @Override
    public Surface getSurface() {
        if (surface == null) {
            surface = new Surface(preSurface);
        }
        return surface;
    }

    @Override
    public boolean hasSurface() {
        return surface != null;
    }

    @Override
    public void setVideoEncoder(IVideoEncoder encoder) {

    }

    @Override
    public Bitmap captureStillImage(int width, int height) {
        return null;
    }

    @Override
    public void setAspectRatio(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0 / height);
    }

    @Override
    public double getAspectRatio() {
        return ratio;
    }
}
