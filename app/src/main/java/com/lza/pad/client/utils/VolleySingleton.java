package com.lza.pad.client.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.lza.pad.client.db.facade.CacheImageFacade;
import com.lza.pad.client.db.model.CacheImage;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by xiads on 14-9-1.
 */
public class VolleySingleton {

    private static VolleySingleton mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;
    private String mCacheImageType;

    private VolleySingleton(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public int getSequenceNumber() {
        return mRequestQueue.getSequenceNumber();
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {

            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
        return mImageLoader;
    }

    public ImageLoader getImageLoader(String type) {
        mCacheImageType = type;
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {

            @Override
            public Bitmap getBitmap(String url) {
                //从数据库获取缓存文件路径
                String filePath = CacheImageFacade.queryByTypeAndKey(mCtx, mCacheImageType, url);
                //实例化Bitmap对象
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                return bitmap;
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                String filePath = CacheImageFacade.queryByTypeAndKey(mCtx, mCacheImageType, url);
                if (TextUtils.isEmpty(filePath)) {
                    //获取缓存文件夹路径
                    File cacheDir = FileTools.createCacheFile(mCacheImageType);
                    //获取文件名
                    //String fileName = FileTools.getFileNameFromUrl(url);
                    String fileName = String.valueOf(System.currentTimeMillis() + ".jpg");
                    //将url作为文件名
                    //String fileName = url;
                    File cacheFile = new File(cacheDir, fileName);
                    if (!cacheFile.exists()) {
                        //保存图片到文件路径
                        try {
                            if (cacheFile.createNewFile()) {
                                FileOutputStream fos = new FileOutputStream(cacheFile);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();
                            }
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                        //保存缓存文件url和文件路径到数据库
                        CacheImage data = new CacheImage();
                        data.setType(mCacheImageType);
                        data.setKey(url);
                        data.setValue(cacheFile.getAbsolutePath());
                        CacheImageFacade.createOrUpdateData(mCtx, data);
                    }
                }
            }
        });
        return mImageLoader;
    }

}
