package com.alex.find.thread;

import android.util.Log;

import com.alex.find.constant.Config;
import com.alex.find.utils.ByteUtil;
import com.alex.find.utils.TimeUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Alex on 2017/4/11.
 */

public class SniffThread extends Thread {
    private DatagramSocket socket;
    private int mPort;
    private boolean isSniff = true;

    public SniffThread(int sniffPort) {
        super();
        mPort = sniffPort;

        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(Config.UDP_LOCAL_PORT)); //local port:5002
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = getSniffMessage();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            packet.setAddress(getBroadcastAddress());
            packet.setPort(mPort);
            while (isSniff) {
                Log.d("SNIFF", "Message:" + TimeUtil.getCurrentDateTime());
                socket.send(packet);
                sleep(5000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            socket.close();
            socket = null;
        }
    }

    public void stopSniff() {
        Log.i("SNIFF", "stopSniff()被调用了");
        isSniff = false;
    }

    private byte[] getSniffMessage() {
        String msg1 = "$CMD";
        int length = msg1.getBytes().length + 1/*数据长度*/ + 1/*数据类型*/;
        String data4 = String.valueOf(Config.HTTP_PORT);
        length += data4.length();

        byte[] buffer = new byte[length];
        //消息头 4字节
        byte[] base1 = msg1.getBytes();
        System.arraycopy(base1, 0, buffer, 0, 4);
        //数据长度 1字节
        String hex = Integer.toHexString(length);
        byte[] base2 = ByteUtil.hexStringToBytes(hex);
        System.arraycopy(base2, 0, buffer, 4, 1);
        //数据类型 1字节
        byte[] base3 = ByteUtil.hexStringToBytes("01");
        System.arraycopy(base3, 0, buffer, 5, 1);
        //数据内容 N字节
        byte[] base4 = data4.getBytes();
        System.arraycopy(base4, 0, buffer, 6, base4.length);

        return buffer;
    }

    private InetAddress getBroadcastAddress() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            return interfaceAddress.getBroadcast();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getBroadcastAddressInString() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            return interfaceAddress.getBroadcast().toString().substring(1);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "192.168.255.255";
    }
}
