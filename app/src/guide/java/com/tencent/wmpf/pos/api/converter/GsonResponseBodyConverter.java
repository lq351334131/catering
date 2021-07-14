package com.tencent.wmpf.pos.api.converter;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.tencent.wmpf.pos.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by gaochujia on 2020-12-25.
 */

public class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    GsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        String s = getString(value.byteStream());
        try {
            JSONObject jsonObject = new JSONObject(s);
            int code = jsonObject.optInt("code");
            String msg = jsonObject.optString("msg");
            if (code != 0) {
                JSONObject newJson = new JSONObject();
                newJson.put("code", code);
                newJson.put("msg", msg);
                newJson.put("data", null);
                s = newJson.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(s)) {
            s = formatStringForPHP(s);
        }
        JsonReader jsonReader = gson.newJsonReader(getReader(s));
        try {
            return adapter.read(jsonReader);
        } finally {
            value.close();
        }
    }

    private String getString(InputStream is) throws IOException {

        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuffer buffer = new StringBuffer();
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        }
    }

    private Reader getReader(String s) {
        Reader reader = new StringReader(s);
        return reader;
    }

    private String formatStringForPHP(String origin) {
        String preRegEx = ",\"[a-zA-Z0-9_]+?\":\\[]";
        String tailRegEx = "\"[a-zA-Z0-9_]+?\":\\[],";
        Pattern r_pre = Pattern.compile(preRegEx);
        Matcher m_pre = r_pre.matcher(origin);
        m_pre.reset();
        StringBuffer sb = new StringBuffer();
        while (m_pre.find()) {
            m_pre.appendReplacement(sb, "");
            LogUtil.log("", "发现空数组直接过滤了");
        }
        m_pre.appendTail(sb);
        Pattern pattern = Pattern.compile(tailRegEx);
        Matcher matcher = pattern.matcher(sb.toString());
        matcher.reset();
        StringBuffer sbNew = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sbNew, "");
            LogUtil.log("", "发现空数组直接过滤了");
        }
        matcher.appendTail(sbNew);
        return sbNew.toString();
    }
}
