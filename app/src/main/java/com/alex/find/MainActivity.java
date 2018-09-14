package com.alex.find;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alex.find.adapter.DevicesRecyclerViewAdapter;
import com.alex.find.bean.Device;
import com.alex.find.constant.Config;
import com.alex.find.constant.Constant;
import com.alex.find.thread.DiscoverThread;
import com.alex.find.thread.HandleThread;
import com.alex.find.utils.NetworkUtil;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements DevicesRecyclerViewAdapter.OnListInteractionListener {
    private static final String TAG = "MainActivity";
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_server_status)
    TextView hint;
    @BindView(R.id.list)
    RecyclerView list;
    @BindView(R.id.tv_version)
    TextView tvVersion;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.load)
    AVLoadingIndicatorView load;
    private MainHandler handler;
    private boolean isServerPrepared = false;
    private boolean isFirstStart = true;
    private List<Device> mDevices = new ArrayList<>();
    private UpdateReceiver updateReceiver;
    private DevicesRecyclerViewAdapter adapter;
    private DiscoverThread discover;
    private ServerSocket server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        tvVersion.setText(getString(R.string.tips_version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE));

        if (mDevices.size() <= 0) {
            list.setVisibility(View.INVISIBLE);
            load.setVisibility(View.VISIBLE);
            load.show();
        }
        adapter = new DevicesRecyclerViewAdapter(mDevices, this);
        list.setLayoutManager(new LinearLayoutManager(this));
        //mDevices.add(Device.createDummy());
        list.setAdapter(adapter);

        registerDeviceUpdateReceiver();

        initOtaServer();

        handler = new MainHandler(this);

        try {
            server = new ServerSocket(Config.TCP_LISTEN_PORT);
            discover = new DiscoverThread(server, this, Config.UDP_REMOTE_PORT);
            discover.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "DiscoverThread启动失败");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        discover.interrupt();
        discover.stopDiscover();
        discover = null;

        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        unregisterDeviceUpdateReceive();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initOtaServer() {
        String ip = NetworkUtil.getLocalIp(getApplicationContext());
        if (TextUtils.isEmpty(ip)) {
            Message msg = new Message();
            msg.what = Constant.MSG.GET_NETWORK_ERROR;
            handler.sendMessage(msg);
            isServerPrepared = false;
        } else {
            hint.setText(getString(R.string.tips_server_prepared, ip, Config.HTTP_PORT));
            isServerPrepared = true;
        }
    }

    private void unregisterDeviceUpdateReceive() {
        if (null != updateReceiver) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        }
    }

    private void registerDeviceUpdateReceiver() {
        updateReceiver = new UpdateReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.MSG_DEVICE_ADD);
        filter.addAction(Constant.MSG_DEVICE_REMOVE);
        filter.addAction(Constant.MSG_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);
    }

    @Override
    public void onListInteraction(Device item) {
        Toast.makeText(this, "设备:" + item.ip, Toast.LENGTH_SHORT).show();
        HandleThread r = discover.pool.get(item.ip);
        if (null != r) {
            //TODO 请改进与线程通讯方法
            r.sendMessage("Sing a song for me.\n");
        }
    }

    private static class MainHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        private MainHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = weakReference.get();
            if (activity == null) return;

            switch (msg.what) {
                case Constant.MSG.GET_NETWORK_ERROR:
                    activity.hint.setText("手机网络地址获取失败，即将退出程序");
                    AlertDialog alert = new AlertDialog.Builder(activity)
                            .setMessage("获取IP失败,请检查Wi-Fi是否开启并连接")
                            .setPositiveButton(R.string.action_settings, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                                    activity.startActivity(wifiSettingsIntent);
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finish();
                                }
                            }).create();
                    alert.show();
                    break;
            }
        }
    }

    private class UdpBroadcastTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog mProgressDialog;
        private InetAddress mInetAddress;
        private boolean isCanceled = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getString(R.string.tips_send_push_stream_message));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isCanceled = true;
                        }
                    });

            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            DatagramSocket socket = null;
            try {
                mInetAddress = InetAddress.getByName(params[0]);

                socket = new DatagramSocket(8000);
                socket.setBroadcast(true);

                Log.e(TAG, "BC IP:" + mInetAddress.getHostAddress());
                byte[] buffer = params[2].getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                packet.setAddress(mInetAddress);
                packet.setPort(Integer.parseInt(params[1]));
                if (!isCanceled) {
                    socket.send(packet);
                }

                Thread.sleep(500); //Call sleep in case of too fast to display
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (null != socket) socket.close();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mProgressDialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            this.cancel(true);
            mProgressDialog.dismiss();
        }

    }

    private class UpgradeAllTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgressDialog;
        private boolean isCanceled = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getString(R.string.tips_send_push_stream_message));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isCanceled = true;
                        }
                    });

            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Iterator ite = discover.pool.entrySet().iterator();
            while (ite.hasNext()) {
                Map.Entry entry = (Map.Entry) ite.next();

                HandleThread ht = (HandleThread) entry.getValue();
                if (null != ht) {
                    String ip = (String) entry.getKey();
                    for (Device orig : mDevices) {
                        if (orig.ip.equals(ip)) {
                            if ("播放".equals(orig.getStatus()) | "播放中".equals(orig.getStatus())) {
                                ht.sendMessage("$AMD快点播放");
                            }
                            break; //已经找到相同device，没有必要继续循环了
                        }
                    }
                }
            }

            try {
                Thread.sleep(500); //Call sleep in case of too fast to display
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mProgressDialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            this.cancel(true);
            mProgressDialog.dismiss();
        }

    }

    private class UpdateReceiver extends BroadcastReceiver {
        private boolean isNewDevice = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("ALEX", "收到广播:" + intent.getAction());

            String action = intent.getAction();
            if (null == action) return;

            switch (action) {
                case Constant.MSG_DEVICE_REMOVE: {
                    Device nd = (Device) intent.getSerializableExtra("devices");

                    for (int i = 0; i < mDevices.size(); i++) {
                        if (mDevices.get(i).ip.equals(nd.ip)) {
                            mDevices.remove(i);
                            adapter.notifyItemChanged(i);

                            if (mDevices.size() <= 0) {
                                list.setVisibility(View.INVISIBLE);
                                load.setVisibility(View.VISIBLE);
                                load.show();
                            }

                            break; //已经找到相同device，没有必要继续循环了
                        }
                    }

                }
                break;
                case Constant.MSG_DEVICE_ADD: {
                    Device nd = (Device) intent.getSerializableExtra("devices");

                    for (int i = 0; i < mDevices.size(); i++) {
                        if (mDevices.get(i).ip.equals(nd.ip)) {
                            isNewDevice = false;
                            Log.e("ALEX", "设备已存在:" + nd.ip + "version:v" + nd.version);
                            mDevices.get(i).reset(nd);
                            adapter.notifyItemChanged(i);

                            break; //已经找到相同device，没有必要继续循环了
                        }
                    }

                    if (isNewDevice) {
                        Log.e("ALEX", "新设备加入:" + nd.ip);
                        mDevices.add(nd);
                        //adapter.notifyDataSetChanged();
                        if (mDevices.size() >= 0) {
                            list.setVisibility(View.VISIBLE);
                            load.setVisibility(View.INVISIBLE);
                            load.hide();
                        }
                        adapter.notifyItemInserted(mDevices.size() - 1);
                    }

                }
                break;
                default: {
                    Log.e("ALEX", "未定义广播:" + intent.getAction());
                }
                break;
            }
        }
    }
}
