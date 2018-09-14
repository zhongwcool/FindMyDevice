package com.alex.find.thread;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alex on 2017/4/11.
 */

public class DiscoverThread extends Thread {
    //private ExecutorService pool;
    public ConcurrentHashMap<String, HandleThread> pool;
    private boolean isListen = true;
    private Context mContext;
    private SniffThread sniff;
    private ServerSocket server;

    public DiscoverThread(ServerSocket socket, Context context, int sniffPort) {
        server = socket;
        mContext = context;

        sniff = new SniffThread(sniffPort);
        sniff.start();

        //pool = Executors.newCachedThreadPool();
        pool = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        while (isListen) {
            try {
                Socket socket = server.accept();
                //TODO 限制线程的数量?
                //pool.execute(new HandleThread(socket));
                HandleThread dog = new HandleThread(mContext, socket);
                pool.put(socket.getInetAddress().getHostAddress(), dog);
                dog.start();
                Log.d("DISCOVER", "新设备:" + socket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopDiscover() {
        Log.i("DISCOVER", "stopDiscover()被调用了");
        isListen = false;

        //停止嗅探
        if (null != sniff) {
            sniff.stopSniff();
        }

        //遍历，并停止每一个线程
        Iterator<Map.Entry<String, HandleThread>> iterator = pool.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, HandleThread> entry = iterator.next();
            entry.getValue().interrupt();
            entry.getValue().stopHandle();
            //pool.remove(entry.getKey());
            iterator.remove();
        }
    }
}
