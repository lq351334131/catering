package com.tencent.wmpf.pos.api;

import com.androidnetworking.interceptors.HttpLoggingInterceptor;
import com.tencent.wmpf.pos.MyApplication;
import com.tencent.wmpf.pos.api.converter.CustomerGsonConverterFactory;
import com.tencent.wmpf.pos.api.interceptor.GzipRequestInterceptor;
import com.tencent.wmpf.pos.sunmi.utils.Constants;
import com.tencent.wmpf.pos.sunmi.utils.SharePreferenceUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Created by gaochujia on 2020-12-25.
 */

public class Api {

    /**
     * Retrofit初始化
     *
     * @return Retrofit
     */
    public static Retrofit createApi() {

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().
                connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);
        httpClientBuilder.addInterceptor(chain -> {
            Request original = chain.request();
            // Request customization: add request headers
            Request.Builder requestBuilder = original.newBuilder()
                    .addHeader("authorization", (String) Objects.requireNonNull(SharePreferenceUtil.getParam(MyApplication.app, Constants.TOKEN, "")));
            Request request = requestBuilder.build();
            return chain.proceed(request);
        });
        httpClientBuilder.addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY));
        httpClientBuilder.addInterceptor(new GzipRequestInterceptor());
        return new Retrofit.Builder()
                .client(httpClientBuilder.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(CustomerGsonConverterFactory.create())
                .baseUrl(Urls.BASE_URL)
                .build();
    }
}
