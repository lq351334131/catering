package com.tencent.wmpf.pos.utils;

import android.util.Log;


/**
 * Created by gaochujia on 2020-12-25.
 */

public class LogUtil {

    /**
     * 日志记录
     */
    public static boolean isDebug = true;

    public static void log(String tag, String message) {

        if (!isDebug|| tag == null || message == null) {
            return;
        }
        Log.e(tag, message);
    }
}
