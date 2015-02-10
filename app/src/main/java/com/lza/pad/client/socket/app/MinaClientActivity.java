package com.lza.pad.client.socket.app;

import android.app.Activity;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.lza.pad.client.R;
import com.lza.pad.client.helper.GsonHelper;
import com.lza.pad.client.socket.admin.client.ClientMessageHandlerAdapter;
import com.lza.pad.client.socket.admin.client.MinaClientAdmin;
import com.lza.pad.client.socket.admin.client.OnClientIoAdapter;
import com.lza.pad.client.socket.model.MinaClient;
import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.wifi.admin.WifiAdmin;

import org.apache.mina.core.session.IoSession;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/2/2.
 */
public class MinaClientActivity extends Activity {

    EditText mEdtIp, mEdtPort, mEdtMessage;
    Button mBtnConnect, mBtnDisconnect, mBtnSendMessage;
    TextView mTxtLogMessage;

    MinaClientAdmin mClientAdmin;

    String mIpServer, mMessage;
    int mPortServer = 8888;

    WifiManager mWifiManager;
    WifiAdmin mWifiAdmin;

    MinaClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_layout);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiAdmin = new WifiAdmin(this);

        mEdtIp = (EditText) findViewById(R.id.client_ip);
        mEdtPort = (EditText) findViewById(R.id.client_port);
        mBtnConnect = (Button) findViewById(R.id.client_connect);
        mBtnDisconnect = (Button) findViewById(R.id.client_disconnect);
        mTxtLogMessage = (TextView) findViewById(R.id.client_log);
        mEdtMessage = (EditText) findViewById(R.id.client_message);
        mBtnSendMessage = (Button) findViewById(R.id.client_send_message);

        mBtnDisconnect.setEnabled(false);

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIpServer = mEdtIp.getText().toString();
                mPortServer = Integer.parseInt(mEdtPort.getText().toString());
                connect(mIpServer, mPortServer);
            }
        });

        mBtnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        mBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessage = mEdtMessage.getText().toString();
                send(mMessage);
            }
        });

        mClientAdmin = new MinaClientAdmin();
        mClientAdmin.setOnClientIoListener(mListener);

        getServerIp();
    }

    private static final int REQUEST_START_CLIENT_SUCCESSFULLY = 0x01;
    private static final int REQUEST_START_CLIENT_FAILED = 0x02;
    private static final int REQUEST_STOP_CLIENT_SUCCESSFULLY = 0x03;
    private static final int REQUEST_STOP_CLIENT_FAILED = 0x04;
    private static final int REQUEST_SEND_MESSAGE = 0x04;

    Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_START_CLIENT_SUCCESSFULLY) {
                mBtnConnect.setEnabled(false);
                mBtnDisconnect.setEnabled(true);
                appendLog("客户端初始化成功！");
                //向服务端请求连接
                mClient = new MinaClient();
                mClient.setSession(mClientAdmin.getSession());
                mClient.setName("夏冬珊");
                mClient.setAcademy("信息学院");
                mClient.setAction(MinaClient.ACTION_CONNECT);
                Gson gson = GsonHelper.builder().excludeFieldsWithoutExposeAnnotation().create();
                String json = gson.toJson(mClient, MinaClient.class);
                AppLogger.e(json);
                send(json);
            } else if (msg.what == REQUEST_START_CLIENT_FAILED) {
                appendLog("客户端初始化失败！");
            } else if (msg.what == REQUEST_STOP_CLIENT_SUCCESSFULLY) {
                mBtnConnect.setEnabled(true);
                mBtnDisconnect.setEnabled(false);
                appendLog("与服务端断开成功！");
            } else if (msg.what == REQUEST_STOP_CLIENT_FAILED) {
                appendLog("与服务端断开失败！");
            } else if (msg.what == REQUEST_SEND_MESSAGE) {
                appendLog("发送消息：" + mEdtMessage);
            }
        }
    };
    public void appendLog(final String msg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mTxtLogMessage.append(wrap(msg));
            }
        });
    }

    public String wrap(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
        String prefix = sdf.format(new Date());
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append(msg);
        sb.append("\n");
        return sb.toString();
    }

    public void disconnect() {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return mClientAdmin.close();
            }

            @Override
            protected void onPostExecute(Boolean ret) {
                if (ret) {
                    mMainHandler.sendEmptyMessage(REQUEST_STOP_CLIENT_SUCCESSFULLY);
                } else {
                    mMainHandler.sendEmptyMessage(REQUEST_STOP_CLIENT_FAILED);
                }
            }
        };
        task.execute();
    }

    public void connect(final String ip, final int port) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return mClientAdmin.connect(ip, port);
            }

            @Override
            protected void onPostExecute(Boolean ret) {
                if (ret) {
                    mMainHandler.sendEmptyMessage(REQUEST_START_CLIENT_SUCCESSFULLY);
                } else {
                    mMainHandler.sendEmptyMessage(REQUEST_START_CLIENT_FAILED);
                }
            }
        };
        task.execute();
    }

    public void send(final String message) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                mClientAdmin.send(message);
                return null;
            }
        };
        task.execute();
    }

    ClientMessageHandlerAdapter.OnClientIoListener mListener = new OnClientIoAdapter() {

        @Override
        public void onExceptionCaught(IoSession session, Throwable cause) {
            appendLog("客户端发生异常：" + cause.getCause() + "," + cause.getMessage());
        }

        @Override
        public void onMessageReceived(IoSession session, Object message) {
            appendLog("客户端接收到[" + session.getRemoteAddress() + "]消息：" + message);
        }

        @Override
        public void onMessageSent(IoSession session, Object message) {
            appendLog("客户端向[" + session.getRemoteAddress() + "]发送消息：" + message);
        }

        @Override
        public void onSessionCreated(IoSession session) {
            appendLog("客户端建立连接[" + session.getRemoteAddress() + "]");
        }

        @Override
        public void onSessionOpened(IoSession session) {
            appendLog("客户端打开连接[" + session.getRemoteAddress() + "]");
        }

        @Override
        public void onSessionClosed(IoSession session) {
            appendLog("客户端关闭连接[" + session.getRemoteAddress() + "]");
        }
    };

    private void getServerIp() {
        if (TextUtils.isEmpty(mIpServer)) {
            //获取服务端的IP
            DhcpInfo dhcp = mWifiManager.getDhcpInfo();
            int ipInt = dhcp.ipAddress;
            if (ipInt == 0) return;
            AppLogger.e("Ip address:" + ipInt);

            mIpServer = String.valueOf(new StringBuilder()
                    .append((ipInt & 0xff)).append('.').append((ipInt >> 8) & 0xff)
                    .append('.').append((ipInt >> 16) & 0xff).append('.')
                    .append(((ipInt >> 24) & 0xff)).toString());
            AppLogger.e("Server Ip:" + mIpServer);
            if (TextUtils.isEmpty(mIpServer)) return;

            int index = mIpServer.lastIndexOf(".");
            String _ipServer = mIpServer.substring(0, index);
            AppLogger.e("_ipServer" + mIpServer);
            mIpServer = new String(new StringBuilder().append(_ipServer).append(".1"));
            AppLogger.e("Server Ip:" + mIpServer);
        }

        mWifiAdmin.createWifiLock();//创建Wifi锁
        mWifiAdmin.acquireWifiLock();//锁定服务器连接

        mEdtIp.setText(mIpServer);
        mEdtPort.setText(String.valueOf(mPortServer));
    }
}
