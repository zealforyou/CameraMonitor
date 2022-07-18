package cn.zz.cameraapp.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import cn.zz.cameraapp.BaseApp;

/**
 * <p>Created by ZhangZhuo on 2022/7/14.</p>
 */
public class WifiUtil {
    public static String getLocalIp(){
        WifiManager wifiManager = (WifiManager) BaseApp.app.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        Log.i("www", "int ip "+ipAddress);
        if(ipAddress==0)return null;
        return ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."
                +(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff));
    }
}
