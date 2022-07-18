package cn.zz.cameraapp.utils;

import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.File;

import cn.zz.cameraapp.BaseApp;

/**
 * <p>Created by ZhangZhuo on 2022/7/14.</p>
 */
public class AndroidStorageUtil {
    /**
     * 获得SD卡总大小
     *
     * @return
     */
    public static String getSDTotalSize() {
        return Formatter.formatFileSize(BaseApp.getApp(), getSDTotalSizeLong());
    }

    public static Long getSDTotalSizeLong() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return blockSize * totalBlocks;
    }


    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    public static String getSDAvailableSize() {
        return Formatter.formatFileSize(BaseApp.getApp(), getSDAvailableSizeLong());
    }

    public static long getSDAvailableSizeLong() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return blockSize * availableBlocks;
    }

}
