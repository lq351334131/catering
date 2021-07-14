package com.tencent.wmpf.pos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PrinterUtils {


    public static final String CONTENT_TYPE_TEXT = "txt";
    public static final String CONTENT_TYPE_BMP = "bmp";
    public static final String CONTENT_TYPE_BARCODE = "one-dimension";
    public static final String CONTENT_TYPE_QRCODE = "two-dimension";


    public static final String POSITION_LEFT = "left";
    public static final String POSITION_CENTER = "center";
    public static final String POSITION_RIGHT = "right";

    public static final int SIZE_LARGH = 3;
    public static final int SIZE_MID = 2;
    public static final int SIZE_SMALL = 1;

    public static final int HEIGHT_LARGH = 3;
    public static final int HEIGHT_MID = 2;
    public static final int HEIGHT_SMALL = 1;


    private static final String KEY_CONTENT = "content";
    private static final String KEY_CONTENT_TYPE = "contenttype";
    private static final String KEY_CONTENT_SIZE = "size";
    private static final String KEY_CONTENT_POSITION = "position";
    private static final String KEY_CONTENT_HEIGHT = "height";

    public static Printer getPrinter() {
        return new Printer();
    }


    private static JSONObject create() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject;
    }

    /**
     * 打印工具类
     * 支持两种方式设置打印
     * 可以采用内封装的形式来添加打印内容 例如：printer=printer.text().fontNormal().alignLeft().text(text);
     * 也可以直接调用打印方法 例如:printer=printer.text(font,align,underline,autoTrunc,text);
     */
    public static class Printer {

        private JSONObject jsonObject;
        private JSONArray ja;

        public Printer() {
            jsonObject = new JSONObject();
            ja = new JSONArray();
            try {
                jsonObject.put("spos", ja);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        public Printer parse(String json) {
            try {
                parse(new JSONObject(json));
            } catch (JSONException e) {
                try {
                    parse(new JSONArray(json));
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
            return this;
        }

        public Printer parse(JSONObject jsonObject) {
            if (jsonObject != null && jsonObject.length() != 0) {
                if (jsonObject.optJSONArray("spos") != null) {
                    parse(jsonObject.optJSONArray("spos"));
                } else {
                    ja.put(jsonObject);
                }
            }
            return this;
        }

        public Printer parse(JSONArray spos) {
            if (spos != null && spos.length() != 0) {
                for (int i = 0; i < spos.length(); i++) {
                    ja.put(spos.opt(i));
                }
            }
            return this;
        }


        public TextPrinter text() {
            return new TextPrinter(this);
        }

        public Printer text(String text, int size, String position) {
            JSONObject jsonObject = create();
            try {
                jsonObject.put(KEY_CONTENT_TYPE, CONTENT_TYPE_TEXT);
                jsonObject.put(KEY_CONTENT, text);
                jsonObject.put(KEY_CONTENT_POSITION, position);
                jsonObject.put(KEY_CONTENT_SIZE, size);
                ja.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        public BarCodePrinter barCode() {
            return new BarCodePrinter(this);
        }

        public Printer barCode(String barcode, int height, String position) {
            JSONObject jsonObject = create();
            try {
                jsonObject.put(KEY_CONTENT_TYPE, CONTENT_TYPE_BARCODE);
                jsonObject.put(KEY_CONTENT, barcode);
                jsonObject.put(KEY_CONTENT_POSITION, position);
                jsonObject.put(KEY_CONTENT_HEIGHT, height);
                ja.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }


        public QrCodePrinter qrCode() {
            return new QrCodePrinter(this);
        }

        public Printer qrCode(String qrCode, int height, String position) {
            JSONObject jsonObject = create();
            try {
                jsonObject.put(KEY_CONTENT_TYPE, CONTENT_TYPE_QRCODE);
                jsonObject.put(KEY_CONTENT, qrCode);
                jsonObject.put(KEY_CONTENT_POSITION, position);
                jsonObject.put(KEY_CONTENT_HEIGHT, height);
                ja.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        public ImagePrinter image() {
            return new ImagePrinter(this);
        }

        public Printer imageFile(String path, int height, String position) {
            JSONObject jsonObject = create();
            try {
                jsonObject.put(KEY_CONTENT_TYPE, CONTENT_TYPE_BMP);
                jsonObject.put(KEY_CONTENT, path);
                jsonObject.put(KEY_CONTENT_POSITION, position);
                jsonObject.put(KEY_CONTENT_HEIGHT, height);
                ja.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }
        public Printer imageBase64(String base64, int height, String position) {
            JSONObject jsonObject = create();
            try {
                jsonObject.put(KEY_CONTENT_TYPE, CONTENT_TYPE_BMP);
                jsonObject.put(KEY_CONTENT, "base64://"+base64);
                jsonObject.put(KEY_CONTENT_POSITION, position);
                jsonObject.put(KEY_CONTENT_HEIGHT, height);
                ja.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }


        public JSONObject toJson() {
            return jsonObject;
        }


        public static class ImagePrinter {
            private Printer printer;
            private String position;
            private int height;

            public ImagePrinter(Printer printer) {
                this.printer = printer;
                heightNormal();
                alignCenter();
            }

            public ImagePrinter heightSmall() {
                height = HEIGHT_SMALL;
                return this;
            }

            public ImagePrinter heightNormal() {
                height = HEIGHT_MID;
                return this;
            }

            public ImagePrinter heightLargh() {
                height = HEIGHT_LARGH;
                return this;
            }

            public ImagePrinter alignCenter() {
                position = POSITION_CENTER;
                return this;
            }

            public ImagePrinter alignLeft() {
                position = POSITION_LEFT;
                return this;
            }

            public ImagePrinter alignRight() {
                position = POSITION_RIGHT;
                return this;
            }

            public Printer imageBase64(byte[] image) {
                return printer.imageBase64(Base64Utils.Base64ByteToString(image), height, position);
            }
            public Printer imageBase64(String base64) {
                return printer.imageBase64(base64, height, position);
            }
            public Printer imageFile(String filePath) {
                return printer.imageFile(filePath, height, position);
            }

            public Printer imageAsset(Context context, String assetName) {
                byte[] bytes = null;
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(assetName));
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    bytes = byteArrayOutputStream.toByteArray();
                    try {
                        byteArrayOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return imageBase64(bytes);
            }
            //
            public Printer imageRes(Context context, int imageRes) {
                Bitmap bitmap = BitmapUtils.getImage(context, imageRes);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return imageBase64(bytes);
            }


        }

        public static class QrCodePrinter {
            private Printer printer;
            private String position;
            private int height;

            public QrCodePrinter(Printer printer) {
                this.printer = printer;
                heightNormal();
                alignCenter();
            }

            public QrCodePrinter heightSmall() {
                height = HEIGHT_SMALL;
                return this;
            }

            public QrCodePrinter heightNormal() {
                height = HEIGHT_MID;
                return this;
            }

            public QrCodePrinter heightLargh() {
                height = HEIGHT_LARGH;
                return this;
            }

            public QrCodePrinter alignCenter() {
                position = POSITION_CENTER;
                return this;
            }

            public QrCodePrinter alignLeft() {
                position = POSITION_LEFT;
                return this;
            }

            public QrCodePrinter alignRight() {
                position = POSITION_RIGHT;
                return this;
            }

            public Printer qrcode(String barCode) {
                return printer.qrCode(barCode, height, position);
            }
        }

        public static class BarCodePrinter {
            private Printer printer;
            private String position;
            private int height;

            public BarCodePrinter(Printer printer) {
                this.printer = printer;
                heightNormal();
                alignCenter();
            }

            public BarCodePrinter heightSmall() {
                height = HEIGHT_SMALL;
                return this;
            }

            public BarCodePrinter heightNormal() {
                height = HEIGHT_MID;
                return this;
            }

            public BarCodePrinter heightLargh() {
                height = HEIGHT_LARGH;
                return this;
            }

            public BarCodePrinter alignCenter() {
                position = POSITION_CENTER;
                return this;
            }

            public BarCodePrinter alignLeft() {
                position = POSITION_LEFT;
                return this;
            }

            public BarCodePrinter alignRight() {
                position = POSITION_RIGHT;
                return this;
            }

            public Printer barCode(String barCode) {
                return printer.barCode(barCode, height, position);
            }

        }

        public static class TextPrinter {
            private Printer printer;
            private int size;
            private String position;

            public TextPrinter(Printer printer) {
                this.printer = printer;
                fontNormal();
                alignLeft();
            }

            public TextPrinter fontSmall() {
                size = SIZE_SMALL;
                return this;
            }

            public TextPrinter fontNormal() {
                size = SIZE_MID;
                return this;
            }

            public TextPrinter fontLarge() {
                size = SIZE_LARGH;
                return this;
            }

            public TextPrinter alignCenter() {
                position = POSITION_CENTER;
                return this;
            }

            public TextPrinter alignLeft() {
                position = POSITION_LEFT;
                return this;
            }

            public TextPrinter alignRight() {
                position = POSITION_RIGHT;
                return this;
            }

            public Printer text(String text) {
                return printer.text(text, size, position);
            }

        }
    }


}
