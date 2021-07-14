package com.tencent.wmpf.pos.api

import com.tencent.wmpf.pos.base.BaseNetworkResponse
import com.tencent.wmpf.pos.bean.response.UserInfoBean
import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by gaochujia on 2020-12-25.
 */
interface ApiService {

    @GET
    fun loginByPwd(@retrofit2.http.Url requestUrl: String, @QueryMap map: Map<String, String>): Observable<BaseNetworkResponse<UserInfoBean>>

    @GET
    fun smsSend(@retrofit2.http.Url requestUrl: String, @Query("mobile")mobile: String): Observable<BaseNetworkResponse<Void>>

    @GET
    fun loginBySms(@retrofit2.http.Url requestUrl: String, @QueryMap map: Map<String, String>): Observable<BaseNetworkResponse<UserInfoBean>>
}