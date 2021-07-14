package com.tencent.wmpf.pos.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.tencent.wmpf.pos.MyApplication;
import com.tencent.wmpf.pos.sunmi.utils.Constants;
import com.tencent.wmpf.pos.sunmi.utils.SharePreferenceUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by gary.gao on 2021/1/7
 */
public class FileUtil {

    public static final String picPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/pos/";
    private static String mSaveMessage = "";

    private static Bitmap bitmap;
    public static void downLoadPic(String url, String fileName){
        new Thread(() -> {
            URL imageUrl = null;
            try {
                imageUrl = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                assert imageUrl != null;
                HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
                saveFile(bitmap, fileName);
                is.close();
            } catch (IOException e) {
                mSaveMessage = "保存失败";
                e.printStackTrace();
            }
        }).start();
    }

    private static void saveFile(Bitmap bm, String fileName) throws IOException {
        File dirFile = new File(picPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File picFile = new File(picPath + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(picFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
        mSaveMessage = "保存成功";
        SharePreferenceUtil.setParam(MyApplication.app, Constants.IMAGE_NAME, fileName);
        messageHandler.sendEmptyMessage(0);
    }

    public void downloadFile(String url){
        final long startTime = System.currentTimeMillis();
        LogUtil.log("DOWNLOAD","startTime="+startTime);
        OkHttpClient okHttpClient = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
                LogUtil.log("DOWNLOAD","download failed");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    File file = new File(savePath, url.substring(url.lastIndexOf("/") + 1));
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
//                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
//                    listener.onDownloadSuccess();
                    LogUtil.log("DOWNLOAD","download success");
                    LogUtil.log("DOWNLOAD","totalTime="+ (System.currentTimeMillis() - startTime));
                } catch (Exception e) {
                    e.printStackTrace();
//                    listener.onDownloadFailed();
                    LogUtil.log("DOWNLOAD","download failed");
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }


    @SuppressLint("HandlerLeak")
    private static Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //ToastUtil.INSTANCE.showToast(mSaveMessage);
        }
    };

    public static String getFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(
                    context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null) {
                Result += line;
            }
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean checkAppInstalled( Context context, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> info = packageManager.getInstalledPackages(0);
        if(info == null || info.isEmpty())
            return false;
        for ( int i = 0; i < info.size(); i++ ) {
            if(pkgName.equals(info.get(i).packageName)) {
                return true;
            }
        }
        return false;
    }
}
