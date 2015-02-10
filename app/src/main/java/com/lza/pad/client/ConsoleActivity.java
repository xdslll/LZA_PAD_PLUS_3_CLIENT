package com.lza.pad.client;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.widgets.DrawableView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/1/28.
 */
public class ConsoleActivity extends Activity {

    TextView mTxtTips;
    EditText mEdtX, mEdtY;
    Button mBtnSend, mBtnServer, mBtnClient;
    LinearLayout mLayout;

    DrawableView mDrawableView;

    boolean mIsServer = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.console);
        mLayout = (LinearLayout) findViewById(R.id.console_layout);
        mTxtTips = (TextView) findViewById(R.id.console_tips);
        mDrawableView = (DrawableView) findViewById(R.id.console_drawableview);

        mEdtX = (EditText) findViewById(R.id.console_x);
        mEdtY = (EditText) findViewById(R.id.console_y);
        mBtnSend = (Button) findViewById(R.id.console_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrX = mEdtX.getText().toString();
                mStrY = mEdtY.getText().toString();
                performTouch();
            }
        });

        mBtnServer = (Button) findViewById(R.id.console_server);
        mBtnClient = (Button) findViewById(R.id.console_client);

        mBtnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                mLayout.setVisibility(View.GONE);
                mIsServer = true;
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            ServerSocket server = new ServerSocket(8888);
                            AppLogger.e("服务端启动成功！端口号：8888");
                            while (true) {
                                Socket client = server.accept();
                                new Thread(new ServerThread(client)).start();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                task.execute();
            }
        });

        mBtnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsServer = false;
                mLayout.setVisibility(View.GONE);
                sendMsgToServer();
                mStrX = mEdtX.getText().toString();
                mStrY = mEdtY.getText().toString();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void sendMsgToServer() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Socket client = new Socket("192.168.31.189", 8888);
                    new Thread(new ClientThread(client)).start();
                    PrintStream out = new PrintStream(client.getOutputStream());
                    String msg = "{\"x\":" + mStrX + ",\"y\":" + mStrY + "}";
                    out.println(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        task.execute();
    }

    String mStrX, mStrY;
    private void performTouch() {
        StringBuilder cmd = new StringBuilder();
        cmd.append("input tap ").append(mStrX).append(" ").append(mStrY);
        Log.e("TAG", "command --> " + cmd);
        if (isRoot()) {
            execShellCmd(cmd.toString());
        }
    }

    private class ServerThread implements Runnable {

        Socket mClient;
        BufferedReader mIn;

        private ServerThread(Socket client) throws IOException {
            this.mClient = client;
            mIn = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String line;
                while (true) {
                    if (mClient.isClosed()) break;
                    if ((line = mIn.readLine()) != null) {
                        AppLogger.e("收到客户端消息：" + line);
                        PrintStream out = new PrintStream(mClient.getOutputStream());
                        out.println("服务端已收到您的消息");

                        line = line.replace("{", "");
                        line = line.replace("}", "");
                        String[] temp1 = line.split(",");
                        if (temp1 != null) {
                            mStrX = temp1[0].split(":")[1];
                            mStrY = temp1[1].split(":")[1];
                            AppLogger.e("x=" + mStrX + ",y=" + mStrY);
                        }
                        mMainHandler.sendEmptyMessage(REQUEST_PERFORM_TOUCH);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientThread implements Runnable {

        Socket mClient;
        BufferedReader mIn;

        public ClientThread(Socket client) throws IOException {
            this.mClient = client;
            mIn = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String content;
                while ((content = mIn.readLine()) != null) {
                    AppLogger.e(content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final int REQUEST_PERFORM_TOUCH = 0x001;
    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_PERFORM_TOUCH) {
                performTouch();
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTxtTips.setText("x=" + event.getX() + ", y=" + event.getY());
        if (!mIsServer) {
            mStrX = String.valueOf(event.getX());
            mStrY = String.valueOf(event.getY());
            sendMsgToServer();
        }
        return mDrawableView.onTouchEvent(event);
    }

    boolean mHasRoot = false;

    private boolean isRoot() {
        try {
            if (!mHasRoot) {
                //获取Root权限
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("exit\n");
                os.flush();
                int exitValue = process.waitFor();
                if (exitValue == 0) {
                    mHasRoot = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mHasRoot;
    }

    private void execShellCmd(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
