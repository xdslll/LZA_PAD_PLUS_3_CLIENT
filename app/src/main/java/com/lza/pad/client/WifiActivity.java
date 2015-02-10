package com.lza.pad.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.utils.ToastUtils;
import com.lza.pad.client.wifi.admin.WifiAdmin;

import java.util.List;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/1/26.
 */
public class WifiActivity extends Activity {

    public static final String TAG = "TAG";
    Button mBtnConnectWifi, mBtnScanWifi;
    TextView mTxtWifiState;
    ListView mListWifi;

    WifiManager mWifiManager;
    WifiAdmin mWifiAdmin;
    Context mCtx;

    boolean mIsWifiEnable = false;
    int mWifiState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCtx = this;
        mInflater = LayoutInflater.from(mCtx);

        setContentView(R.layout.wifi_hotpot);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mTxtWifiState = (TextView) findViewById(R.id.wifi_hotpot_state);
        mListWifi = (ListView) findViewById(R.id.wifi_hotpot_wifi_list);
        mBtnConnectWifi = (Button) findViewById(R.id.wifi_hotpot_connect);

        mIsWifiEnable = mWifiManager.isWifiEnabled();//获取Wifi开启状态
        wifiStateLog("程序启动");
        setWifiStateByStatus();//设置Wifi状态文字

        mWifiAdmin = new WifiAdmin(mCtx) {
            @Override
            public void onWifiConnected() {
                ToastUtils.showShort(mCtx, "Wifi连接成功！");
            }

            @Override
            public void onWifiConnectTimeout() {
                ToastUtils.showShort(mCtx, "Wifi连接失败！");
            }
        };

        mBtnConnectWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiStateLog("用户点击了开启/关闭按钮");
                mIsWifiEnable = mWifiManager.isWifiEnabled();
                if (mIsWifiEnable) {
                    closeWifi();
                } else {
                    openWifi();
                }
            }
        });

        mBtnScanWifi = (Button) findViewById(R.id.wifi_hotpot_scan);
        mBtnScanWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRetryCount = 0;
                startWifiScan();
            }
        });
    }


    private void setWifiStateByStatus() {
        if (mIsWifiEnable) {
            mTxtWifiState.setText("开启");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("关闭Wifi");
            startWifiScan();
        } else {
            mTxtWifiState.setText("关闭");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("开启Wifi");
            clearWifiList();
        }
    }

    private ProgressDialog mProgressDialog = null;
    private void startWifiScan() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mCtx);
            mProgressDialog.setMessage("正在扫描Wifi列表...");
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
        if (mRetryCount >= 0 && mRetryCount < MAX_RETRY_COUNT) {
            startWifiScan(1000);
        } else {
            mRetryCount = 0;
            ToastUtils.showShort(mCtx, "没有获取到Wifi列表！");
            if (mProgressDialog != null)
                mProgressDialog.dismiss();
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
                    mListWifi.setAdapter(mAdapter);
                    mListWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                    if (mProgressDialog != null)
                        mProgressDialog.dismiss();
                } else {
                    mRetryCount++;
                    startWifiScan();
                }
            }
        }, delay);
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
                //WifiConfiguration config = mWifiAdmin.createWifiInfo(SSID, password, type);
                //mWifiAdmin.addNetwork(config);
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

    List<ScanResult> mWifiList;
    LayoutInflater mInflater;
    WifiListApdater mAdapter;
    int mRetryCount = 0;
    static final int MAX_RETRY_COUNT = 3;

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

    private void wifiStateLog(String msg) {
        Log.e(TAG, "-----------------------------------------------");
        Log.e(TAG, "|\t" + msg);
        Log.e(TAG, "|\twifi state:" + mWifiManager.getWifiState());
        Log.e(TAG, "|\tis wifi enable:" + mIsWifiEnable);
        Log.e(TAG, "-----------------------------------------------");
    }

    private void openWifi() {
        boolean ifSuccess = mWifiAdmin.openWifi();
        if (!ifSuccess) {
            ToastUtils.showShort(mCtx, "Wifi开启失败！");
            return;
        }
        mMainHandler.sendEmptyMessage(REQUEST_OPEN_WIFI);
    }

    private void closeWifi() {
        boolean ifSuccess = mWifiAdmin.closeWifi();
        if (!ifSuccess) {
            ToastUtils.showShort(mCtx, "Wifi关闭失败！");
            return;
        }
        mMainHandler.sendEmptyMessage(REQUEST_CLOSE_WIFI);
    }

    private static final int REQUEST_OPEN_WIFI = 0x1;
    private static final int REQUEST_CLOSE_WIFI = 0x2;
    private static final int REQUEST_HANDLE_OPEN_WIFI_STATE = 0x3;
    private static final int REQUEST_HANDLE_CLOSE_WIFI_STATE = 0x4;

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_OPEN_WIFI) {
                mWifiState = mWifiManager.getWifiState();
                mMainHandler.sendEmptyMessage(REQUEST_HANDLE_OPEN_WIFI_STATE);
            } else if (msg.what == REQUEST_HANDLE_OPEN_WIFI_STATE) {
                handleWifiOpen();
            } else if (msg.what == REQUEST_CLOSE_WIFI) {
                mWifiState = mWifiManager.getWifiState();
                mMainHandler.sendEmptyMessage(REQUEST_HANDLE_CLOSE_WIFI_STATE);
            } else if (msg.what == REQUEST_HANDLE_CLOSE_WIFI_STATE) {
                handleWifiClose();
            }
        }
    };

    private void handleWifiOpen() {
        mWifiState = mWifiManager.getWifiState();
        mIsWifiEnable = mWifiManager.isWifiEnabled();
        if (mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            mTxtWifiState.setText("关闭");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("开启Wifi");
            clearWifiList();
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_OPEN_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLING) {
            mTxtWifiState.setText("正在开启...");
            mBtnConnectWifi.setEnabled(false);
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_OPEN_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            mTxtWifiState.setText("开启");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("关闭Wifi");
            startWifiScan();

            wifiStateLog("wifi开启成功");
        } else {
            ToastUtils.showShort(mCtx, "未知状态，请重试！");
        }
    }

    private void handleWifiClose() {
        mWifiState = mWifiManager.getWifiState();
        mIsWifiEnable = mWifiManager.isWifiEnabled();
        if (mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            mTxtWifiState.setText("关闭");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("开启Wifi");
            clearWifiList();

            wifiStateLog("wifi关闭成功");
        } else if (mWifiState == WifiManager.WIFI_STATE_DISABLING) {
            mTxtWifiState.setText("正在关闭...");
            mBtnConnectWifi.setEnabled(false);
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_CLOSE_WIFI_STATE, 500);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            mTxtWifiState.setText("开启");
            mBtnConnectWifi.setEnabled(true);
            mBtnConnectWifi.setText("关闭Wifi");
            mMainHandler.sendEmptyMessageDelayed(REQUEST_HANDLE_CLOSE_WIFI_STATE, 500);
        } else {
            ToastUtils.showShort(mCtx, "未知状态，请重试！");
        }
    }
}
