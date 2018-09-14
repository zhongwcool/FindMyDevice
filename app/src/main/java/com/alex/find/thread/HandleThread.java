package com.alex.find.thread;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alex.find.bean.Device;
import com.alex.find.constant.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Alex on 2017/4/13.
 */

public class HandleThread extends Thread {
    private final Socket mSocket;
    private LocalBroadcastManager lbm;
    private boolean isListen = true;

    public HandleThread(Context context, Socket socket) {
        lbm = LocalBroadcastManager.getInstance(context);
        mSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            //byte[] data = new byte[10];
            while (isListen) {
                String data = buffer.readLine();
                Log.e("ALEX", "收到的数据:" + data);

                //TODO 可以通知主程序移除设备，连接断开时读取消息为空
                if (null != data) {
                    handleMessage(data);
                } else {
                    String ip = mSocket.getInetAddress().getHostAddress();
                    Device device = new Device(ip);
                    Intent intent = new Intent(Constant.MSG_DEVICE_REMOVE);
                    intent.putExtra("devices", device);
                    lbm.sendBroadcast(intent);

                    //连接断开，移除连接
                    isListen = false;
                }
            }

            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String msg) {
        String ip = mSocket.getInetAddress().getHostAddress();

        if (msg.startsWith("MAC")) {
            if (msg.length() == 39) {
                Device device = new Device(ip, msg.substring(5, 22), trimVersionInfo(msg.substring(28, 39)));
                Intent intent = new Intent(Constant.MSG_DEVICE_ADD);
                //intent.putExtra("devices", Device.createDummy());
                intent.putExtra("devices", device);
                lbm.sendBroadcast(intent);
            } else {
                Device device = Device.createDummy();
                Intent intent = new Intent(Constant.MSG_DEVICE_ADD);
                intent.putExtra("devices", device);
                lbm.sendBroadcast(intent);
                Log.e("ALEX", "非法数据:长度异常:" + msg);
            }
        } else {
            Log.e("ALEX", "非法数据:未定义:" + msg);
        }
    }

    public void stopHandle() {
        Log.e("HANDLE", "stopHandle()被调用了");
        //连接断开，移除连接
        isListen = false;
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int trimProgress(String msg) {
        try {
            return Integer.valueOf(msg.substring(10));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String trimVersionInfo(String msg) {
        Log.d("ALEX", "截取的信息：" + msg);
        StringBuilder builder = new StringBuilder();
        String[] data = msg.split("\\.");
        try {
            for (int i = 0; i < 4; i++) {
                builder.append(Integer.valueOf(data[i]));
                if (i != 3) {
                    builder.append(".");
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "0.0.0.0";
        }

        return builder.toString();
    }

    public void sendMessage(final String msg) {
        //String msg = "UpgradeFirmware:http://"+ mSocket.getInetAddress().getHostAddress()+"/Download";
        new Thread() {
            @Override
            public void run() {
                try {
                    OutputStream os = mSocket.getOutputStream();
                    os.write(msg.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
