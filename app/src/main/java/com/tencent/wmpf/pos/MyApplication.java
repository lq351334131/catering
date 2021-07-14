package com.tencent.wmpf.pos;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.tencent.mmkv.MMKV;
import com.tencent.wmpf.pos.utils.InvokeTokenHelper;
import com.tencent.wmpf.pos.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * For 4.4 multi dex support
 */
public class MyApplication extends Application {
    public static MyApplication app = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

        InvokeTokenHelper.INSTANCE.initInvokeToken(this);

        String rootDir = MMKV.initialize(this);
        System.out.println("mmkv root: " + rootDir);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        // 只需要关注Api类中的方法即可跑通WMPF
        Api.INSTANCE.init(this);
    }

    public static MyApplication getInstance() {
        return app;
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        MMKV.onExit();
    }

    public boolean isHaveCamera() {
        HashMap<String, UsbDevice> deviceHashMap = ((UsbManager) getSystemService(Activity.USB_SERVICE)).getDeviceList();
        for (Map.Entry entry : deviceHashMap.entrySet()) {
            UsbDevice usbDevice = (UsbDevice) entry.getValue();
            if (!TextUtils.isEmpty(usbDevice.getInterface(0).getName()) && usbDevice.getInterface(0).getName().contains("Orb")) {
                return true;
            }
            if (!TextUtils.isEmpty(usbDevice.getInterface(0).getName()) && usbDevice.getInterface(0).getName().contains("Astra")) {
                return true;
            }
        }
        return false;
    }

    public String getUsbInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        for (UsbDevice usb : list.values()) {
            int pid = usb.getProductId();
            int vid = usb.getVendorId();
            String sn = usb.getSerialNumber();
            LogUtil.log("usbInfo", "sn:"+ sn);
            stringBuilder.append(sn).append("\n");
        }
        return stringBuilder.toString();
    }

}
