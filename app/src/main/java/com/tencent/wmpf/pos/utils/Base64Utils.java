package com.tencent.wmpf.pos.utils;

import android.util.Base64;

public class Base64Utils {

    /**
     * byte数组转明文
     * @param b
     * @return
     */
    public static String Base64ByteToString(byte[] b){
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    /**
     * Base64的明文转byte数组
     * @param str
     * @return
     */
    public static byte[] Base64StringToByte(String str){
        return Base64.decode(str, Base64.DEFAULT);
    }
}
