package com.lza.pad.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lza.pad.client.socket.app.MinaClientActivity;
import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.utils.ToastUtils;
import com.lza.pad.client.wifi.admin.WifiAdmin;

import java.util.List;


public class MainWifiActivity extends Activity {

    Button mBtnScan, mBtnSetWifi;
    ListView mListScan;

    WifiManager mWifiManager;
    WifiAdmin mWifiAdmin;

    Context mCtx;

    boolean mIsWifiEnable;
    int mWifiState;

    List<ScanResult> mWifiList;//Wifi列表
    LayoutInflater mInflater;
    WifiListApdater mAdapter;
    int mRetryCount = 0;//当前重试次数
    static final int MAX_RETRY_COUNT = 5;//最多重试次数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        mCtx = this;
        mInflater = LayoutInflater.from(this);
        setContentView(R.layout.activity_main);

        mBtnScan = (Button) findViewById(R.id.scan);
        mBtnSetWifi = (Button) findViewById(R.id.set_wifi);
        mListScan = (ListView) findViewById(R.id.scan_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiAdmin = new WifiAdmin(mCtx) {
            @Override
            public void onWifiConnected() {
                dismissProgressDialog();
                ToastUtils.showShort(mCtx, "大屏连接成功！");
                startActivity(new Intent(mCtx, MinaClientActivity.class));
            }

            @Override
            public void onWifiConnectTimeout() {
                dismissProgressDialog();
                ToastUtils.showShort(mCtx, "大屏连接失败！");
            }
        };

        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        mBtnSetWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainWifiActivity.this, WifiActivity.class));
            }
        });
        scanAvailablePad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        int state = mWifiAdmin.getRegisterState();
        if (state != WifiAdmin.STATE_UNREGISTERED)
            mWifiAdmin.unregister();
        dismissProgressDialog();
        mProgressDialog = null;
    }

    private void scanAvailablePad() {
        mRetryCount = 0;
        //确认Wifi设备已经打开
        mIsWifiEnable = mWifiManager.isWifiEnabled();
        if (!mIsWifiEnable) {
            //showOpenWifiDialog();
            openWifiDirectly();
        } else {
            startWifiScan();
        }

    }

    private void openWifiDirectly() {
        mMainHandler.sendEmptyMessage(REQUEST_OPEN_WIFI);
    }

    private static final int REQUEST_OPEN_WIFI = 0x1;
    private static final int REQUEST_CLOSE_WIFI = 0x2;
    private static final int REQUEST_HANDLE_OPEN_WIFI_STATE = 0x3;
    private static final int REQUEST_HANDLE_CLOSE_WIFI_STATE = 0x4;

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_OPEN_WIFI) {
                boolean ret = mWifiAdmin.openWifi();
                if (ret) {
                    mMainHandler.sendEmptyMessage(REQUEST_HANDLE_OPEN_WIFI_STATE);
                    showProgressDialog("正在打开Wifi...");
                } else
                    ToastUtils.showShort(mCtx, "Wifi设备启动失败！");
            } else if (msg.what == REQUEST_HANDLE_OPEN_WIFI_STATE) {
                handleWifiOpen();
            } else if (msg.what == REQUEST_CLOSE_WIFI) {
                boolean ret = mWifiAdmin.closeWifi();
                if (ret)
                    mMainHandler.sendEmptyMessage(REQUEST_HANDLE_CLOSE_WIFI_STATE);
                else
                    ToastUtils.showShort(mCtx, "Wifi设备关闭失败！");
            } else if (msg.what == REQUEST_HANDLE_CLOSE_WIFI_STATE) {
                handleWifiClose();
            }
        }
    };

    private void handleWifiOpen() {
        mWifiState = mWifiManager.getWifiState();
        mIsWifiEnable = mWifiManager.isWifiEnabled();
        if (mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            clearWifiList();
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_OPEN_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLING) {
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_OPEN_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            dismissProgressDialog();
            startWifiScan();
        } else {
            dismissProgressDialog();
            ToastUtils.showShort(mCtx, "未知状态，请重试！");
        }
    }

    private void handleWifiClose() {
        mWifiState = mWifiManager.getWifiState();
        mIsWifiEnable = mWifiManager.isWifiEnabled();
        if (mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            dismissProgressDialog();
            clearWifiList();
        } else if (mWifiState == WifiManager.WIFI_STATE_DISABLING) {
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_CLOSE_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_CLOSE_WIFI_STATE, 500);
        } else {
            dismissProgressDialog();
            ToastUtils.showShort(mCtx, "未知状态，请重试！");
        }
    }

    private class WifiListApdater extends BaseAdapter {

        @Override
        public int getCount() {
            return mWifiList.size();
        }

        @Override
        public ScanResult getItem(int position) {
            return mWifiList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.wifi_hotpot_list_item, null);
                holder.txtSSID = (TextView) convertView.findViewById(R.id.wifi_hotpot_list_item_ssid);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.txtSSID.setText(getItem(position).SSID);
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView txtSSID;
    }

    private ProgressDialog mProgressDialog = null;

    private void showProgressDialog(String msg, boolean ifDismiss) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }
        if (ifDismiss && mProgressDialog.isShowing()) {
            dismissProgressDialog();
        }
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    private void showProgressDialog(String msg) {
        showProgressDialog(msg, true);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void startWifiScan() {
        showProgressDialog("正在扫描Wifi列表...", false);

        if (mRetryCount >= 0 && mRetryCount < MAX_RETRY_COUNT) {
            AppLogger.e("正在扫描Wifi列表,次数:" + (mRetryCount + 1) + "次.");
            startWifiScan(1000);
        } else {
            mRetryCount = 0;
            ToastUtils.showShort(mCtx, "没有获取到Wifi列表！");
            dismissProgressDialog();
        }
    }

    private void startWifiScan(int delay) {
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWifiAdmin.startScan();
                mWifiList = mWifiAdmin.getWifiList();
                if (mWifiList != null && mWifiList.size() > 0) {
                    mAdapter = new WifiListApdater();
                    mListScan.setAdapter(mAdapter);
                    mListScan.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            ScanResult data = mWifiList.get(position);
                            String SSID = data.SSID;
                            String TYPE = data.capabilities;
                            AppLogger.e("SSID:" + SSID + ",TYPE:" + TYPE);
                            int type;
                            if (TYPE.contains("WPA") || TYPE.contains("WPA2")) {
                                type = WifiAdmin.TYPE_WPA;
                                showWifiPasswordDialog(SSID, type);
                            } else if (TYPE.contains("WEP")) {
                                type = WifiAdmin.TYPE_WEP;
                                showWifiPasswordDialog(SSID, type);
                            } else {
                                type = WifiAdmin.TYPE_NO_PASSWORD;
                                mWifiAdmin.addNetWork(SSID, "", type);
                            }
                        }
                    });
                    connectToPad();
                } else {
                    mRetryCount++;
                    startWifiScan();
                }
            }
        }, delay);
    }

    private void connectToPad() {
        boolean hasPadPlus = false;
        if (mWifiList != null) {
            for (ScanResult wifi : mWifiList) {
                String SSID = wifi.SSID;
                if (SSID.contains("南京大学")) {
                    hasPadPlus = true;
                    showProgressDialog("开始连接大屏...");
                    String password = "1234567890";
                    mWifiAdmin.addNetWork(SSID, password, WifiAdmin.TYPE_WPA);
                    break;
                }
            }
        }
        if (!hasPadPlus)
            ToastUtils.showShort(mCtx, "附近没有大屏！");
    }

    private void showWifiPasswordDialog(final String SSID, final int type) {
        View view = mInflater.inflate(R.layout.wifi_hotpot_list_password, null);
        final AlertDialog dialog = new AlertDialog.Builder(mCtx).setView(view).create();

        final EditText edtPassword = (EditText) view.findViewById(R.id.wifi_hotpot_list_item_password);
        Button btnConfirm = (Button) view.findViewById(R.id.wifi_hotpot_list_item_confirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = edtPassword.getText().toString();
                mWifiAdmin.addNetWork(SSID, password, type);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void clearWifiList() {
        if (mWifiList != null && mAdapter != null) {
            mWifiList.clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void showOpenWifiDialog() {
        new AlertDialog.Builder(mCtx)
                .setTitle("提示")
                .setMessage("检测到您的Wifi设备已关闭，是否现在打开？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMainHandler.sendEmptyMessage(REQUEST_OPEN_WIFI);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
