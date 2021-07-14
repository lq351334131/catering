package com.tencent.wmpf.pos.sunmi.present;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.sunmi.devicemanager.cons.PrtCts;
import com.sunmi.devicemanager.device.Device;
import com.sunmi.devicesdk.core.PrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.sunmi.peripheral.printer.WoyouConsts;
import com.sunmi.sunmiopenservice.SunmiOpenServiceWrapper;
import com.tencent.wmpf.event.CallbackEvent;
import com.tencent.wmpf.pos.R;
import com.tencent.wmpf.pos.activity.TestActivity;
import com.tencent.wmpf.pos.sunmi.bean.MenuBean;
import com.tencent.wmpf.pos.sunmi.bean.SunmiLink;
import com.tencent.wmpf.pos.sunmi.utils.BitmapUtils;
import com.tencent.wmpf.pos.sunmi.utils.Constants;
import com.tencent.wmpf.pos.sunmi.utils.ResourcesUtils;
import com.tencent.wmpf.pos.sunmi.utils.Utils;
import com.tencent.wmpf.pos.utils.FileUtil;
import com.tencent.wmpf.pos.utils.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by zhicheng.liu on 2018/4/4
 * address :liuzhicheng@sunmi.com
 * description :
 */

public class PrinterPresenter {
    private Context context;
    private static final String TAG = "PrinterPresenter";
    public SunmiPrinterService printerService;
    private PrinterManager mManager;

    public PrinterPresenter(Context context, SunmiPrinterService printerService) {
        this.context = context;
        this.printerService = printerService;
        mManager = PrinterManager.getInstance();
    }

    public void print(final String json, final int payMode) {
        if (printerService == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                MenuBean menuBean = JSON.parseObject(json, MenuBean.class);
                int fontsizeTitle = 40;
                int fontsizeContent = 30;
                int fontsizeFoot = 35;
                String divide = "**************************************" + "\n";
                String divide2 = "--------------------------------------" + "\n";
                if (TestActivity.isVertical) {
                    divide = "************************" + "\n";
                    divide2 = "------------------------" + "\n";
                }
                int width = divide2.length();
                String goods = formatTitle(width);
                try {
                    if (printerService.updatePrinterState() != 1) {
                        return;
                    }
                    if (payMode == Constants.PAY_MODE_0) {
                        try {
                            printerService.openDrawer(null);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    printerService.setAlignment(1, null);
                    printerService.sendRAWData(boldOn(), null);
                    printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.menus_title) + "\n" + ResourcesUtils.getString(context, R.string.print_proofs) + "\n", "", fontsizeTitle, null);
                    printerService.setAlignment(0, null);
                    printerService.sendRAWData(boldOff(), null);
                    printerService.printTextWithFont(divide, "", fontsizeContent, null);
                    printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_order_number) + SystemClock.uptimeMillis() + "\n", "", fontsizeContent, null);
                    printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_order_time) + formatData(new Date()) + "\n", "", fontsizeContent, null);
                    printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_payment_method), "", fontsizeContent, null);
                    switch (payMode) {
                        case Constants.PAY_MODE_0:
                            printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.pay_money) + "\n", "", fontsizeContent, null);
                            break;
                        case Constants.PAY_MODE_5:
                        case Constants.PAY_MODE_2:
                            printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.pay_face) + "\n", "", fontsizeContent, null);
                            break;
                        case Constants.PAY_MODE_1:
                        case Constants.PAY_MODE_3:
                        case Constants.PAY_MODE_4:
                            printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.pay_code) + "\n", "", fontsizeContent, null);
                            break;
                        default:
                            break;
                    }
                    printerService.printTextWithFont(divide, "", fontsizeContent, null);
                    printerService.printTextWithFont(goods + "\n", "", fontsizeContent, null);
                    printerService.printTextWithFont(divide2, "", fontsizeContent, null);
                    printGoods(menuBean, fontsizeContent, divide2, payMode, width);

                    printerService.printTextWithFont(divide, "", fontsizeContent, null);
                    printerService.sendRAWData(boldOn(), null);
                    if (payMode != 0 && payMode != 1) {
                        printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_tips_havemoney), "", fontsizeFoot, null);
                    } else {
                        printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_tips_nomoney), "", fontsizeFoot, null);
                    }
                    printerService.sendRAWData(boldOff(), null);
                    printerService.lineWrap(3, null);

                    String wifi = SunmiOpenServiceWrapper.getInstance().getSunmilinkDynamicInfo();
//                    wifi = "{\"type\":\"password\",\"data\":{\"ssid\":\"无线牛逼\",\"password\":\"12345678\",\"qrcode\":\"\",\"expires_in\":0}}";
                    Log.d(TAG, "run: " + wifi);
                    if (!TextUtils.isEmpty(wifi)) {
                        printerService.setAlignment(1, null);
                        SunmiLink sunmiLink = JSON.parseObject(wifi, SunmiLink.class);
                        printerService.printTextWithFont("Wi-Fi" + (isZh() ? "名称:" : ":") + sunmiLink.getData().getSsid() + "\n", "", fontsizeContent, null);
                        printerService.printTextWithFont((isZh() ? "Wi-Fi密码:" : "Password:") + sunmiLink.getData().getPassword(), "", fontsizeContent, null);
                        printerService.lineWrap(2, null);
                       /*  printerService.printQRCode(wifi, 6, 30, null);
                        printerService.lineWrap(1, null);
                        printerService.printTextWithFont(ResourcesUtils.getString(R.string.print_sunmilink_tips), "", fontsizeContent, null);
                        printerService.lineWrap(2, null);*/
                        printerService.setAlignment(0, null);

                    }
                    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.print_logo);
                    if (bitmap.getWidth() > 384) {
                        int newHeight = (int) (1.0 * bitmap.getHeight() * 384 / bitmap.getWidth());
                        bitmap = BitmapUtils.scale(bitmap, 384, newHeight);
                    }
                    printerService.printBitmap(bitmap, null);
                    printerService.printText("\n\n", null);
                    printerService.printTextWithFont(ResourcesUtils.getString(context, R.string.print_thanks), "", fontsizeContent, null);

                    printerService.lineWrap(4, null);
                    printerService.cutPaper(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //打印任意json
    public void print2(final String json, final int payMode) {
        if (printerService == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //获取打印机纸张规格
                    int mm = printerService.getPrinterPaper();
                    if (printerService.updatePrinterState() != 1) {
                        return;
                    }
                    if (payMode == Constants.PAY_MODE_0) {
                        //目前由前端控制
                        /*try {
                            //打开钱箱
                            printerService.openDrawer(null);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }*/

                        JSONObject object = new JSONObject(json);
                        JSONArray array;
                        if (object.optJSONArray("spos") != null) {
                            array = object.optJSONArray("spos");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject data = (JSONObject) array.opt(i);
                                LogUtil.log("printData", data.toString());
                                printerService.sendRAWData(boldOff(), null);
                                printerService.setPrinterStyle( WoyouConsts.ENABLE_ILALIC, WoyouConsts.DISABLE);
                                printerService.setAlignment(0, null);
                                switch (data.optString("contenttype")) {
                                    case "txt":
                                        //设置粗体
                                        if (data.optInt("bold") == 1) {
                                            printerService.sendRAWData(boldOn(), null);
                                        }
                                        //设置斜体
                                        /*if (data.optInt("italic") == 1) {
                                            printerService.setPrinterStyle(WoyouConsts.ENABLE_ILALIC, WoyouConsts.ENABLE);
                                        }*/
                                        printerService.setAlignment(data.optInt("position"), null);
                                        printerService.printTextWithFont(data.optString("content") + "\n", "", data.optInt("size"), null);
                                        break;
                                    case "line":
                                        printerService.setAlignment(data.optInt("position"), null);
                                        if (mm == 1)
                                            printerService.printText("--------------------------------\n", null);
                                        else
                                            printerService.printText("------------------------------------------------\n", null);
                                        break;
                                    case "bmp":
                                        printerService.lineWrap(1, null);
                                        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.print_logo);
                                        Bitmap bitmap= BitmapFactory.decodeFile(FileUtil.picPath + data.optString("content"));
                                        int pixel = mm == 1? 384 : 576;
                                        if (bitmap != null) {
                                            if (bitmap.getWidth() > pixel) {
                                                int newHeight = (int) (1.0 * bitmap.getHeight() * pixel / bitmap.getWidth());
                                                bitmap = BitmapUtils.scale(bitmap, pixel, newHeight);
                                            }
                                            printerService.setAlignment(data.optInt("position"), null);
                                            printerService.printBitmap(bitmap, null);
                                            printerService.lineWrap(2, null);
                                        }
                                        break;
                                    case "one-dimension":
                                        printerService.setAlignment(data.optInt("position"), null);
                                        printerService.printBarCode(data.optString("content"),  8, data.optInt("height"), data.optInt("width"), data.optInt("position"), null);
                                        printerService.printText("\n", null);
                                        break;
                                    case "two-dimension":
                                        printerService.setAlignment(data.optInt("position"), null);
                                        printerService.printQRCode(data.optString("content"), data.optInt("qrsize"), 3, null);
                                        printerService.printText("\n", null);
                                        break;
                                }
                            }
                        }
                    }
                    printerService.cutPaper(null);
                    EventBus.getDefault().post(new CallbackEvent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void printByDeviceManager(String json, int payMode, Device device) {
        if (mManager == null) return;
        mManager.setDefaultDevice(device);
        mManager.enter(true);
        mManager.initPrinter();
        MenuBean menuBean = JSON.parseObject(json, MenuBean.class);
        try {
            mManager.addBold(true);
            mManager.addTextSizeDouble();
            mManager.addTextAtCenter(ResourcesUtils.getString(context, R.string.menus_title));
            mManager.addTextAtCenter(ResourcesUtils.getString(context, R.string.print_proofs));
            mManager.addTextSizeNormal();
            mManager.addBold(false);
            mManager.addFeedDots(10);
            mManager.addHorizontalCharLine('*');
            mManager.addText(ResourcesUtils.getString(context, R.string.print_order_number) + SystemClock.uptimeMillis());
            mManager.addFeedDots(10);
            mManager.addText(ResourcesUtils.getString(context, R.string.print_order_time) + formatData(new Date()));
            mManager.addFeedDots(10);
            String mode = null;
            switch (payMode) {
                case Constants.PAY_MODE_0:
                    mode = ResourcesUtils.getString(context, R.string.pay_money);
                    break;
                case Constants.PAY_MODE_5:
                case Constants.PAY_MODE_2:
                    mode = ResourcesUtils.getString(context, R.string.pay_face);
                    break;
                case Constants.PAY_MODE_1:
                case Constants.PAY_MODE_3:
                case Constants.PAY_MODE_4:
                    mode = ResourcesUtils.getString(context, R.string.pay_code);
                    break;
                default:
                    break;
            }
            mManager.addText(ResourcesUtils.getString(context, R.string.print_payment_method) + mode);
            mManager.addHorizontalCharLine('*');
            mManager.addFeedDots(10);
            String[] title = {
                    ResourcesUtils.getString(context, R.string.shop_car_goods_name),
                    ResourcesUtils.getString(context, R.string.menus_unit_price),
                    ResourcesUtils.getString(context, R.string.menus_unit_num),
                    ResourcesUtils.getString(context, R.string.shop_car_unit_money),
            };
            mManager.addTextsAutoWrap(new float[]{3, 1, 2, 2},
                    new int[]{PrtCts.ALIGN_LEFT, PrtCts.ALIGN_CENTER, PrtCts.ALIGN_CENTER, PrtCts.ALIGN_RIGHT}
                    , title);
            mManager.addFeedDots(10);
            mManager.addHorizontalLine(0);
            mManager.addFeedDots(10);
            printGoodsByDeviceManager(menuBean);
            mManager.addFeedDots(10);
            mManager.addHorizontalCharLine('*');
            mManager.addBold(true);
            mManager.addTextSizeDouble();
            if (payMode != 0 && payMode != 1) {
                mManager.addText(ResourcesUtils.getString(context, R.string.print_tips_havemoney));
            } else {
                mManager.addText(ResourcesUtils.getString(context, R.string.print_tips_nomoney));
            }
            mManager.addTextSizeDouble();
            mManager.addBold(false);
            mManager.addFeedLine(3);

            String wifi = SunmiOpenServiceWrapper.getInstance().getSunmilinkDynamicInfo();
            if (!TextUtils.isEmpty(wifi)) {
                mManager.addAlignCenter();
                SunmiLink sunmiLink = JSON.parseObject(wifi, SunmiLink.class);
                mManager.addText("Wi-Fi" + (isZh() ? "名称:" : ":") + sunmiLink.getData().getSsid());
                mManager.addText((isZh() ? "Wi-Fi密码:" : "Password:") + sunmiLink.getData().getPassword());
                mManager.addFeedLine(2);
                mManager.addAlignLeft();
            }

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.print_logo);
            if (bitmap.getWidth() > 384) {
                int newHeight = (int) (1.0 * bitmap.getHeight() * 384 / bitmap.getWidth());
                bitmap = BitmapUtils.scale(bitmap, 384, newHeight);
            }
            mManager.addImage(bitmap);
            mManager.addFeedLine(1);
            mManager.addText(ResourcesUtils.getString(context, R.string.print_thanks));
            mManager.addFeedLine(6);
            mManager.addCutter();
            mManager.commit(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printGoodsByDeviceManager(MenuBean menuBean) {
        for (MenuBean.ListBean listBean : menuBean.getList()) {
            int num;
            if (listBean.getType() == 1) {
                num = (int) (listBean.getNet() / 1000.000f);
            } else {
                num = 1;
            }
            mManager.addTextsAutoWrap(new float[]{3, 1, 2, 2},
                    new int[]{PrtCts.ALIGN_LEFT, PrtCts.ALIGN_CENTER, PrtCts.ALIGN_CENTER, PrtCts.ALIGN_RIGHT},
                    new String[]{listBean.getParam2(),
                            listBean.getParam3().replace(ResourcesUtils.getString(context, R.string.units_money), ""),
                            num + "",
                            listBean.getParam3().replace(ResourcesUtils.getString(context, R.string.units_money), "").trim()});
            mManager.addFeedDots(10);
        }
        mManager.addHorizontalLine(0);
        mManager.addFeedDots(10);
        String total = ResourcesUtils.getString(context, R.string.print_total_payment);
        String real = ResourcesUtils.getString(context, R.string.print_real_payment);
        mManager.addTextLeftRight(total, ResourcesUtils.getString(context, R.string.units_money_units) + menuBean.getKVPList().get(0).getValue());
        mManager.addFeedDots(10);
        mManager.addTextLeftRight(real, ResourcesUtils.getString(context, R.string.units_money_units) + Constants.PayMoney);
    }

    private String formatTitle(int width) {
        Log.e("@@@@@", width + "=======");

        String[] title = {
                ResourcesUtils.getString(context, R.string.shop_car_goods_name),
                ResourcesUtils.getString(context, R.string.menus_unit_price),
                ResourcesUtils.getString(context, R.string.menus_unit_num),
                ResourcesUtils.getString(context, R.string.shop_car_unit_money),
        };
        StringBuffer sb = new StringBuffer();
        int blank1 = width / 3 - String_length(title[0]);
        int blank2 = width / 4 - String_length(title[1]);
        int blank3 = width / 4 - String_length(title[2]);

        sb.append(title[0]);
        sb.append(addblank(blank1));

        sb.append(title[1]);
        sb.append(addblank(blank2));

        sb.append(title[2]);
        sb.append(addblank(blank3));

        sb.append(title[3]);

        return sb.toString();
    }

    private void printNewline(String str, int width, int fontsizeContent) throws RemoteException {
        List<String> strings = Utils.getStrList(str, width);
        for (String string : strings) {
            printerService.printTextWithFont(string + "\n", "", fontsizeContent, null);
        }
    }

    private void printGoods(MenuBean menuBean, int fontsizeContent, String divide2, int payMode, int width) throws RemoteException {
        int blank1;
        int blank2;
        int blank3;
        int maxNameWidth = isZh() ? (width / 3 - 2) / 2 : (width / 3 - 2);

        StringBuffer sb = new StringBuffer();
        for (MenuBean.ListBean listBean : menuBean.getList()) {
            sb.setLength(0);

            String name = listBean.getParam2();
            String name1 = name.length() > maxNameWidth ? name.substring(0, maxNameWidth) : "";

            blank1 = width / 3 - String_length(name.length() > maxNameWidth ? name1 : name) + 1;

            blank2 = width / 4 - String_length(listBean.getParam3().replace(ResourcesUtils.getString(context, R.string.units_money), ""));

            sb.append(name.length() > maxNameWidth ? name1 : name);
            sb.append(addblank(blank1));

            sb.append(listBean.getParam3().replace(ResourcesUtils.getString(context, R.string.units_money), ""));
            sb.append(addblank(blank2));

            if (listBean.getType() == 1) {
                sb.append(listBean.getNet() / 1000.000f);
                blank3 = width / 4 - (listBean.getNet() / 1000.000f + "").length();
            } else {
                sb.append(1);
                blank3 = width / 4 - 1;
            }

            sb.append(addblank(blank3));
            sb.append(listBean.getParam3());
            printerService.printTextWithFont(sb.toString() + "\n", "", fontsizeContent, null);

            if (name.length() > maxNameWidth) {
                printNewline(name.substring(maxNameWidth), maxNameWidth, fontsizeContent);
            }

        }
        printerService.printTextWithFont(divide2, "", fontsizeContent, null);
        String total = ResourcesUtils.getString(context, R.string.print_total_payment);
        String real = ResourcesUtils.getString(context, R.string.print_real_payment);

        sb.setLength(0);
        blank1 = width * 5 / 6 - String_length(total) - menuBean.getKVPList().get(0).getValue().length();
        blank2 = width * 5 / 6 - String_length(real) - menuBean.getKVPList().get(0).getValue().length();
        ;
        sb.append(total);
        sb.append(addblank(blank1));
        sb.append(ResourcesUtils.getString(context, R.string.units_money_units));
        sb.append(menuBean.getKVPList().get(0).getValue());

        printerService.printTextWithFont(sb.toString() + "\n", "", fontsizeContent, null);
        sb.setLength(0);
        sb.append(real);
        sb.append(addblank(blank2));
        sb.append(ResourcesUtils.getString(context, R.string.units_money_units));

        sb.append(Constants.PayMoney);

        printerService.printTextWithFont(sb.toString() + "\n", "", fontsizeContent, null);
        sb.setLength(0);
    }

    private String formatData(Date nowTime) {
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return time.format(nowTime);
    }

    private String addblank(int count) {
        String st = "";
        if (count < 0) {
            count = 0;
        }
        for (int i = 0; i < count; i++) {
            st = st + " ";
        }
        return st;
    }

    private static final byte ESC = 0x1B;// Escape

    /**
     * 字体加粗
     */
    private byte[] boldOn() {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 69;
        result[2] = 0xF;
        return result;
    }

    /**
     * 取消字体加粗
     */
    private byte[] boldOff() {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 69;
        return result;
    }

    private boolean isZh() {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        return language.endsWith("zh");
    }

    private int String_length(String rawString) {
        return rawString.replaceAll("[\\u4e00-\\u9fa5]", "SH").length();
    }


}
