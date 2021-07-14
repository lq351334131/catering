package com.tencent.wmpf.pos.api

import com.tencent.wmpf.pos.BuildConfig


/**
 * Created by gaochujia on 2020-12-25.
 */
object Urls {

    /**
     * 接口地址
     */
    const val BASE_URL = BuildConfig.APIURL //接口域名

    const val LOGIN = "$BASE_URL/uam/login" //登录

    const val GET_SIGN= "$BASE_URL/store/wmpf/getSign" //获取签名

    const val GET_IMAGE= "$BASE_URL/pos/ticketInfo/searchImage" //获取小票图片

    const val VERSION = ""  //版本更新
}