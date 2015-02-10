package com.lza.pad.client;

import android.app.Activity;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.wifi.admin.WifiAdmin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/1/28.
 */
public class SocketTestActivity extends Activity {

    WifiManager mWifiManager;
    WifiAdmin mWifiAdmin;

    Button mBtnSend;
    EditText mEdtText;
    TextView mTxtMsg;

    Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiAdmin = new WifiAdmin(this);

        setContentView(R.layout.socket_test);
        mEdtText = (EditText) findViewById(R.id.socket_client_text);
        mTxtMsg = (TextView) findViewById(R.id.socket_client_msg);
        mBtnSend = (Button) findViewById(R.id.socket_client_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    boolean mIpReady = false;
    private void sendMessage() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DhcpInfo dhcp = mWifiManager.getDhcpInfo();
                int ipInt = dhcp.ipAddress;
                if (ipInt != 0) {
                    mIpReady = true;
                }
                AppLogger.e("Ip address:" + ipInt);
                if (mIpReady) {
                    mWifiAdmin.createWifiLock();
                    mWifiAdmin.acquireWifiLock();

                    String ipServer = String.valueOf(new StringBuilder()
                            .append((ipInt & 0xff)).append('.').append((ipInt >> 8) & 0xff)
                            .append('.').append((ipInt >> 16) & 0xff).append('.')
                            .append(((ipInt >> 24) & 0xff)).toString());
                    AppLogger.e("Server Ip:" + ipServer);

                    int index = ipServer.lastIndexOf(".");
                    String _ipServer = ipServer.substring(0, index);
                    AppLogger.e("_ipServer" + ipServer);
                    ipServer = new String(new StringBuilder().append(_ipServer).append(".1"));
                    AppLogger.e("Server Ip:" + ipServer);

                    try {
                        Socket socket = new Socket(ipServer, 8888);

                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        final String line = in.readLine();

                        final String text = mEdtText.getText().toString();
                        PrintStream ps = new PrintStream(socket.getOutputStream());
                        ps.println(text);

                        ps.close();
                        in.close();
                        socket.close();
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                displayClient(text);
                                displayServer(line);
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        task.execute();
    }

    private void displayClient(String msg) {
        displayMessage(msg, true);
    }

    private void displayServer(String msg) {
        displayMessage(msg, false);
    }

    private void displayMessage(String msg, boolean ifClient) {
        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss]");
        String date = sdf.format(new Date());
        StringBuilder sb = new StringBuilder();
        if (ifClient) {
            sb.append("Client");
        } else {
            sb.append("Server");
        }
        sb.append(date);
        sb.append(":");
        sb.append(msg);
        sb.append("\n");
        mTxtMsg.append(sb);
        mEdtText.setText("");
    }


}
