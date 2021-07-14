package com.tencent.wmpf.pos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    private static final String TAG = "BitmapUtils";
    public static final int MAX_WIDTH = 1024;
    public static final int MAX_HEIGHT = 768;
    /**
     * 质量压缩方法
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 90;
        while (baos.toByteArray().length / 1024 > 200) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }


    private static BitmapFactory.Options getOpt(BitmapFactory.Options newOpts, int maxWidth, int maxHeight){
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > maxWidth) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / maxWidth);
        } else if (w < h && h > maxHeight) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / maxHeight);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        return newOpts;
    }

    /**
     * 图片按比例大小压缩方法
     * @return
     */
    public static Bitmap getImage(Context context, int resourceId) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(),resourceId, newOpts);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),resourceId, getOpt(newOpts,MAX_WIDTH,MAX_HEIGHT));
        return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
    }


    public static Bitmap getFileImage(Context context, String srcPath){
        return getImage(context,srcPath,false,MAX_WIDTH,MAX_HEIGHT);
    }
    /**
     * 图片按比例大小压缩方法
     * @param srcPath （根据路径获取图片并压缩）
     * @return
     */
    public static Bitmap getImage(Context context, String srcPath, boolean asset, int maxWidth, int maxHeight) {
//        Log.d(TAG,"srcPath="+srcPath);
        try {
            InputStream is = asset ? context.getAssets().open(srcPath) : new FileInputStream(srcPath);

            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is,null, newOpts);

            is = asset ? context.getAssets().open(srcPath) : new FileInputStream(srcPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is,null,getOpt(newOpts,maxWidth,maxHeight));
            return bitmap;//compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
