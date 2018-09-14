package com.alex.find.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by Guo.Duo duo on 2015/9/4.
 */
public class NetworkUtil {
    public static int getLocalIpInInt(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);//获取WifiManager

        //检查wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            return -1;
        }

        WifiInfo wifiinfo = wifiManager.getConnectionInfo();

        return wifiinfo.getIpAddress();
    }

    public static String getLocalIp(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);//获取WifiManager

        //检查wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            return null;
        }

        WifiInfo wifiinfo = wifiManager.getConnectionInfo();

        String ip = intToIp(wifiinfo.getIpAddress());

        return ip;
    }

    private static String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "."
                + (0xFF & paramInt >> 16) + "." + (0xFF & paramInt >> 24);
    }

    public static String getMacFromBytes(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            String sTemp = String.format("%02X", data[i]);
            //String sTemp = Integer.toHexString(0xFF &  data[i]);
            builder.append(sTemp);
            if (i < 5) {
                builder.append(":");
            }
        }

        return builder.toString();
    }

    public static String getVersionFromBytes(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 6; i < 10; i++) {
            int v = data[i] & 0xFF;
            builder.append(v);
            if (i == 7) {
                builder.append(".");
            }
        }

        return builder.toString();
    }
}
