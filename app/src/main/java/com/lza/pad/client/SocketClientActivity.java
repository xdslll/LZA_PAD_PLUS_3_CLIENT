package com.lza.pad.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.lza.pad.client.helper.GsonHelper;
import com.lza.pad.client.utils.AppLogger;
import com.lza.pad.client.utils.ToastUtils;
import com.lza.pad.client.widgets.DrawableView;
import com.lza.pad.client.wifi.admin.WifiAdmin;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/1/29.
 */
public class SocketClientActivity extends Activity {

    DrawableView mDrawableView;
    String mIpServer = null;

    WifiManager mWifiManager;
    WifiAdmin mWifiAdmin;

    Socket mClient = null;

    LinearLayout mLayout;
    EditText mEdtIp;
    Button mBtnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.socket_client);
        mLayout = (LinearLayout) findViewById(R.id.socket_client_layout);
        mEdtIp = (EditText) findViewById(R.id.socket_client_ip);
        mBtnConfirm = (Button) findViewById(R.id.socket_client_confirm);

        mDrawableView = (DrawableView) findViewById(R.id.socket_client_drawableview);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiAdmin = new WifiAdmin(this);

        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIpServer = mEdtIp.getText().toString().trim();
                startService();
            }
        });
    }

    private void startService() {
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

        mLayout.setVisibility(View.GONE);

        initClient();//创建与服务端的连接
    }

    private void initClient() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mClient = new Socket(mIpServer, 8888);
                    AppLogger.e("与服务端连接成功！");
                    mToastMsg = "与服务端连接成功！";
                    mHandler.sendEmptyMessage(REQUEST_SHOW_TOAST);

                    new Thread(new ClientThread(mClient)).start();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        task.execute();
    }

    private void sendMsgToServer() {
        if (TextUtils.isEmpty(mJson)) return;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (mClient == null || mClient.isClosed() || !mClient.isConnected()) {
                        /*mClient = new Socket(mIpServer, 8888);
                        AppLogger.e("与服务端连接成功！");
                        new Thread(new ClientThread(mClient)).start();*/
                        initClient();
                    }
                    PrintStream out = new PrintStream(mClient.getOutputStream());
                    //String msg = "{\"x\":" + mStrX + ",\"y\":" + mStrY + "}";
                    out.println(mJson);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        task.execute();
    }

    private class ClientThread implements Runnable {

        Socket mClient;
        DataInputStream mIn;

        public ClientThread(Socket client) throws IOException {
            this.mClient = client;
            mIn = new DataInputStream(mClient.getInputStream());
        }

        @Override
        public void run() {
            try {
                //String content;
                //while (mClient != null && !mClient.isClosed() && (content = mIn.readLine()) != null) {
                //    AppLogger.e(content);
                //}
                while (mClient != null && !mClient.isClosed() && mClient.isConnected()) {
                    /*int size = mIn.readInt();
                    byte[] data = new byte[size];
                    int len = 0;
                    while (len < size) {
                        len += mIn.read(data, len, size - len);
                    }
                    //ByteArrayOutputStream outPut = new ByteArrayOutputStream();
                    mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    //mBitmap.compress(Bitmap.CompressFormat.PNG, 80, outPut);
                    AppLogger.e("获取图片成功！");
                    mHandler.sendEmptyMessage(REQUEST_UPDATE_IMAGE);*/

                    int type = mIn.readInt();
                    String fileName = mIn.readUTF();
                    long fileLength = mIn.readLong();
                    AppLogger.e("文件名：" + fileName + ",type：" + type + ",length:" + fileLength);

                    if (type == TYPE_SHAKE) {
                        File sdDir = Environment.getExternalStorageDirectory();
                        File file = new File(sdDir, fileName);
                        AppLogger.e("文件存放路径：" + file.getAbsolutePath());

                        mFile = file;
                        mFileName = fileName;
                        mProgressMax = (int) fileLength;
                        mProgress = 0;
                        mHandler.sendEmptyMessage(REQUEST_SHOW_PROGRESS);

                        FileOutputStream out = new FileOutputStream(file);

                        int size = 1024 * 100;
                        byte[] data = new byte[size];
                        int length;
                        while ((length = mIn.read(data, 0, data.length)) > 0) {
                            out.write(data, 0, length);
                            out.flush();
                            mProgress += length;
                            mHandler.sendEmptyMessage(REQUEST_UPDATE_PROGRESS);
                        }
                        AppLogger.e("文件接收完毕！");

                        mHandler.sendEmptyMessage(REQUEST_DISMISS_PROGRESS);
                        mToastMsg = "文件传输成功！存放路径：[" + file.getAbsolutePath() + "]";
                        mHandler.sendEmptyMessage(REQUEST_SHOW_TOAST);
                        mHandler.sendEmptyMessage(REQUEST_OPEN_FILE);
                    } else if (type == TYPE_CAPTURE_SCREEN) {
                        byte[] data = new byte[(int) fileLength];
                        int len = 0;
                        while (len < fileLength) {
                            len += mIn.read(data, len, (int) fileLength - len);
                        }
                        //ByteArrayOutputStream out = new ByteArrayOutputStream();
                        mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        //mBitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
                        AppLogger.e("获取图片成功！");
                        mHandler.sendEmptyMessage(REQUEST_UPDATE_IMAGE);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ProgressDialog mProgressDialog;
    int mProgressMax;
    float mProgress;
    String mFileName;
    String mToastMsg;
    File mFile;

    String mJson;
    private Bitmap mBitmap;
    private static final int REQUEST_UPDATE_IMAGE = 0x01;
    private static final int REQUEST_UPDATE_PROGRESS = 0x02;
    private static final int REQUEST_SHOW_PROGRESS = 0x03;
    private static final int REQUEST_DISMISS_PROGRESS = 0x04;
    private static final int REQUEST_SHOW_TOAST = 0x05;
    private static final int REQUEST_OPEN_FILE = 0x06;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_UPDATE_IMAGE) {
                mDrawableView.setBitmap(mBitmap);
            } else if (msg.what == SENSOR_SHAKE) {
                ToastUtils.showShort(SocketClientActivity.this, "摇一摇成功！");
                Cor message = new Cor();
                message.type = TYPE_SHAKE;
                mJson = GsonHelper.instance().toJson(message);
                AppLogger.e("json : " + mJson);
                sendMsgToServer();
            } else if (msg.what == REQUEST_UPDATE_PROGRESS) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    float rate = mProgress / mProgressMax;
                    mProgressDialog.setMessage("正在传输文件[" + mFileName + "]：" + (int) (rate * 100) + "%");
                }
            } else if (msg.what == REQUEST_SHOW_PROGRESS) {
                mProgressDialog = new ProgressDialog(SocketClientActivity.this);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMessage("正在接收文件...");
                mProgressDialog.setMax(mProgressMax);
                mProgressDialog.setProgress((int) mProgress);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            } else if (msg.what == REQUEST_DISMISS_PROGRESS) {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
            } else if (msg.what == REQUEST_SHOW_TOAST) {
                ToastUtils.showLong(SocketClientActivity.this, mToastMsg);
            } else if (msg.what == REQUEST_OPEN_FILE) {
                new AlertDialog.Builder(SocketClientActivity.this)
                        .setMessage("提示")
                        .setMessage("文件[" + mFileName + "]下载成功！是否现在打开？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mFile == null || !mFile.exists()) return;
                                dialog.dismiss();
                                openFile(mFile);
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
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Cor message = new Cor();
            message.x = (int) event.getX();
            message.y = (int) event.getY();
            message.type = TYPE_TOUCH;
            mJson = GsonHelper.instance().toJson(message);
            AppLogger.e("json : " + mJson);
            sendMsgToServer();
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public void onBackPressed() {
        Cor message = new Cor();
        message.key = 4;
        message.type = TYPE_KEY;
        mJson = GsonHelper.instance().toJson(message);
        AppLogger.e("json : " + mJson);
        sendMsgToServer();
    }

    private static final int TYPE_TOUCH = 1;
    private static final int TYPE_KEY = 2;
    private static final int TYPE_SHAKE = 3;
    private static final int TYPE_CAPTURE_SCREEN = 4;

    private class Cor {
        int x, y, type, key;
    }

    /**
     * 实现摇一摇功能
     */
    private SensorManager mSensorManager;
    private Vibrator mVibrator;
    private static final int SENSOR_SHAKE = 10;

    private void initSensor() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
        initSensor();
        if (mSensorManager != null) {
            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClient = null;
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
    }

    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];
            //AppLogger.e("x轴方向的重力加速度" + x +  "；y轴方向的重力加速度" + y +  "；z轴方向的重力加速度" + z);
            int mediumValue = 19;
            if (Math.abs(x) > mediumValue || Math.abs(y) > mediumValue || Math.abs(z) > mediumValue) {
                mVibrator.vibrate(200);
                Message msg = new Message();
                msg.what = SENSOR_SHAKE;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 打开文件
     * @param file
     */
    private void openFile(File file){

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        //获取文件file的MIME类型
        String type = getMIMEType(file);
        //设置intent的data和Type属性。
        intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
        //跳转
        try {
            startActivity(intent);//这里最好try一下，有可能会报错。 //比如说你的MIME类型是打开邮箱，但是你手机里面没装邮箱客户端，就会报错。
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     * @param file
     */
    private String getMIMEType(File file) {

        String type="*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /*获取文件的后缀名*/
        String end = fName.substring(dotIndex,fName.length()).toLowerCase();
        if (end.equals("")) return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0;i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private final String[][] MIME_MapTable={
            //{后缀名，MIME类型}
            {".3gp",    "video/3gpp"},
            {".apk",    "application/vnd.android.package-archive"},
            {".asf",    "video/x-ms-asf"},
            {".avi",    "video/x-msvideo"},
            {".bin",    "application/octet-stream"},
            {".bmp",    "image/bmp"},
            {".c",      "text/plain"},
            {".class",  "application/octet-stream"},
            {".conf",   "text/plain"},
            {".cpp",    "text/plain"},
            {".doc",    "application/msword"},
            {".docx",   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls",    "application/vnd.ms-excel"},
            {".xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe",    "application/octet-stream"},
            {".epub",   "application/epub+zip"},
            {".gif",    "image/gif"},
            {".gtar",   "application/x-gtar"},
            {".gz",     "application/x-gzip"},
            {".h",      "text/plain"},
            {".htm",    "text/html"},
            {".html",   "text/html"},
            {".jar",    "application/java-archive"},
            {".java",   "text/plain"},
            {".jpeg",   "image/jpeg"},
            {".jpg",    "image/jpeg"},
            {".js",     "application/x-javascript"},
            {".log",    "text/plain"},
            {".m3u",    "audio/x-mpegurl"},
            {".m4a",    "audio/mp4a-latm"},
            {".m4b",    "audio/mp4a-latm"},
            {".m4p",    "audio/mp4a-latm"},
            {".m4u",    "video/vnd.mpegurl"},
            {".m4v",    "video/x-m4v"},
            {".mov",    "video/quicktime"},
            {".mp2",    "audio/x-mpeg"},
            {".mp3",    "audio/x-mpeg"},
            {".mp4",    "video/mp4"},
            {".mpc",    "application/vnd.mpohun.certificate"},
            {".mpe",    "video/mpeg"},
            {".mpeg",   "video/mpeg"},
            {".mpg",    "video/mpeg"},
            {".mpg4",   "video/mp4"},
            {".mpga",   "audio/mpeg"},
            {".msg",    "application/vnd.ms-outlook"},
            {".ogg",    "audio/ogg"},
            {".pdf",    "application/pdf"},
            {".png",    "image/png"},
            {".pps",    "application/vnd.ms-powerpoint"},
            {".ppt",    "application/vnd.ms-powerpoint"},
            {".pptx",   "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop",   "text/plain"},
            {".rc",     "text/plain"},
            {".rmvb",   "audio/x-pn-realaudio"},
            {".rtf",    "application/rtf"},
            {".sh",     "text/plain"},
            {".tar",    "application/x-tar"},
            {".tgz",    "application/x-compressed"},
            {".txt",    "text/plain"},
            {".wav",    "audio/x-wav"},
            {".wma",    "audio/x-ms-wma"},
            {".wmv",    "audio/x-ms-wmv"},
            {".wps",    "application/vnd.ms-works"},
            {".xml",    "text/plain"},
            {".z",      "application/x-compress"},
            {".zip",    "application/x-zip-compressed"},
            {"",        "*/*"}
    };
}
