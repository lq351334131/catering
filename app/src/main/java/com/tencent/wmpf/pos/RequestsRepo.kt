package com.tencent.wmpf.pos

import android.util.Log
import com.google.gson.Gson
import com.tencent.luggage.demo.wxapi.DeviceInfo
import com.tencent.luggage.demo.wxapi.DeviceInfo.KEY_EXPIRED_TIME_MS
import com.tencent.luggage.demo.wxapi.DeviceInfo.KEY_TEST_DEVICE_ID
import com.tencent.luggage.demo.wxapi.DeviceInfo.KEY_TEST_KEY_VERSION
import com.tencent.luggage.demo.wxapi.DeviceInfo.KEY_TEST_PRODUCT_ID
import com.tencent.luggage.demo.wxapi.DeviceInfo.KEY_TEST_SIGNATURE
import com.tencent.wmpf.cli.task.IPCInovkerTask_SetPushMsgCallback
import com.tencent.wmpf.cli.task.IPCInvokerTask_getPushToken
import com.tencent.wmpf.cli.task.pb.WMPFBaseRequestHelper
import com.tencent.wmpf.cli.task.pb.WMPFIPCInvoker
import com.tencent.wmpf.pos.api.Urls
import com.tencent.wmpf.pos.bean.LoginInfo
import com.tencent.wmpf.pos.bean.StoreParam
import com.tencent.wmpf.pos.bean.User
import com.tencent.wmpf.pos.bean.response.ImageBean
import com.tencent.wmpf.pos.ui.PushMsgQuickStartActivity
import com.tencent.wmpf.pos.utils.ConvertUtil
import com.tencent.wmpf.proto.WMPFPushMsgRequest
import com.tencent.wmpf.proto.WMPFPushMsgResponse
import com.tencent.wmpf.proto.WMPFPushTokenRequest
import com.tencent.wmpf.proto.WMPFPushTokenResponse
import okhttp3.*
import org.json.JSONObject
import java.nio.charset.Charset
import okhttp3.RequestBody

/**
 * Created by complexzeng on 2019-10-17 16:25.
 */
object RequestsRepo {
    const val TAG = "RequestsRepo"

    private val client = OkHttpClient()

    fun getAppToken(user: User, callback: (Boolean, String) -> Unit) {
        Thread {
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val body = RequestBody.create(mediaType, Gson().toJson(user))
            val request = Request.Builder().url(Urls.LOGIN).post(body).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    try {
                        val respBody = response.body()!!.source().readString(Charset.defaultCharset())
                        val jsonObject = JSONObject(respBody)
                        val code = jsonObject.optInt("code")
                        val data = jsonObject.optString("data")
                        val msg = jsonObject.optString("message")
                        val loginInfo = ConvertUtil.toObject(data, LoginInfo::class.java)
                        if (code != 0) {
                            callback(false, msg)
                        } else {
                            callback(true, loginInfo.accessToken)
                        }
                    } catch (e: Exception) {
                        callback(false, "网络异常，请稍后再试")
                    }

                } else {
                    callback(false, "网络异常，请稍后再试")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAppToken: ", e)
                callback(false, "网络异常，请稍后再试")
            }
        }.start()
    }

    fun getSign(token: String, deviceId: String, orgId: String, storeId: String, keyVersion: Int, deviceTypeCode: String, deviceName: String, callback: (Boolean, String) -> Unit) {
        Thread {
            val request = Request.Builder()
                    .url(Urls.GET_SIGN + "?deviceId=$deviceId&orgId=$orgId&storeId=$storeId&version=$keyVersion&deviceTypeCode=$deviceTypeCode&deviceName=$deviceName")
                    .addHeader("Authorization", token)
                    .build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    try {
                        val respBody = response.body()!!.source().readString(Charset.defaultCharset())
                        val jsonObject = JSONObject(respBody)
                        val code = jsonObject.optInt("code")
                        val data = jsonObject.optString("data")
                        val msg = jsonObject.optString("message")
                        if (code != 0) {
                            callback(false, msg)
                        } else {
                            callback(true, data)
                        }
                    } catch (e: Exception) {
                        callback(false, "网络异常，请稍后再试")
                    }
                } else {
                    callback(false, "网络异常，请稍后再试")
                }
            } catch (e: Exception) {

            }
        }.start()
    }

    fun getImage(deviceId: String, callback: (Boolean, ArrayList<String>?) -> Unit) {
        Thread {
            val request = Request.Builder()
                    .url(Urls.GET_IMAGE + "?snCode=$deviceId")
                    .build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    try {
                        val respBody = response.body()!!.source().readString(Charset.defaultCharset())
                        val jsonObject = JSONObject(respBody)
                        val code = jsonObject.optInt("code")
                        val data = jsonObject.optJSONArray("data")
                        val list = ArrayList<String>()
                        for (i in 0 until data.length()) {
                            val imageBean = ConvertUtil.toObject(data.get(i).toString(), ImageBean::class.java)
                            list.add(imageBean.image)
                        }
                        if (code != 0) {
                            callback(false, null)
                        } else {
                            callback(true, list)
                        }
                    } catch (e: Exception) {
                        callback(false, null)
                    }
                } else {
                    callback(false, null)
                }
            } catch (e: Exception) {
                callback(false, null)
            }
        }.start()
    }

    fun getAccessToken(callback: (Boolean, String) -> Unit) {
        Thread {
            val request = Request.Builder().url("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=${BuildConfig.HOST_APPID}&secret=${BuildConfig.HOST_APPSECRET}").build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    try {
                        val respBody = response.body()!!.source().readString(Charset.defaultCharset())
                        val jsonObject = JSONObject(respBody)
                        val accessToken = jsonObject.optString("access_token")
                        callback(true, accessToken)
                    } catch (e: Exception) {
                        callback(false, "获取失败...")
                    }

                } else {
                    callback(false, "获取失败...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAccessToken: ", e)
            }
        }.start()
    }


    /**
     * @param ticket  get from wecooper
     * @param wxaAppId get from wecooper
     * only for test
     * never use this function for production environment
     */
    fun getTestDeviceInfo(ticket: String, wxaAppId: String, hostAppId: String, callback: (resp: String) -> Unit) {
        Thread {
            val req = Request.Builder().url("https://open.weixin.qq.com/wxaruntime" +
                    "/getdemodeviceinfo?ticket=$ticket&wxaappid=$wxaAppId&hostappid=$hostAppId").build()
            try {
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val respBody = resp.body()!!.source().readString(Charset.defaultCharset())
                    callback(respBody)
                    val jsonObject = JSONObject(respBody)
                    val productId = jsonObject.optInt(KEY_TEST_PRODUCT_ID, 0)
                    val deviceId = jsonObject.optString(KEY_TEST_DEVICE_ID, "")
                    val signature = jsonObject.optString(KEY_TEST_SIGNATURE, "")
                    val keyVersion = jsonObject.optInt(KEY_TEST_KEY_VERSION, 0)
                    val appIdList = jsonObject.optJSONArray("appid_list")
                    val expiredTimeMs = jsonObject.optLong(KEY_EXPIRED_TIME_MS) * 1000L + System.currentTimeMillis()
                    Log.d(TAG, "getDeviceInfo: productId = $productId, deviceId = $deviceId, signature = $signature, keyVersion = $keyVersion, appIdList = $appIdList, expiredTimeMs = $expiredTimeMs")

                    DeviceInfo.productId = productId
                    DeviceInfo.deviceId = deviceId
                    DeviceInfo.signature = signature
                    DeviceInfo.keyVersion = keyVersion
                    DeviceInfo.expiredTimeMs = expiredTimeMs

                } else {
                    callback("error")
                    Log.w(TAG, "getDeviceInfo fail: ")
                }
            } catch (e: Exception) {
                callback("error: ${e.message.toString()}")
                Log.e(TAG, "getDeviceInfo fail: ${e.message.toString()}")
            }
        }.start()
    }

    fun getPushToken(appId: String, callback: (Boolean, String, Int,String) -> Unit) {
        val request = WMPFPushTokenRequest()
        request.baseRequest = WMPFBaseRequestHelper.checked()
        request.appId = appId
        val result = WMPFIPCInvoker.invokeAsync<
                IPCInvokerTask_getPushToken,
                WMPFPushTokenRequest,
                WMPFPushTokenResponse
                >(request, IPCInvokerTask_getPushToken::class.java) { response ->
            if (response.baseResponse.errCode == 0) {
                callback(true, response.pushToken, response.expireTimestamp,response.baseResponse.errMsg)
            } else {
                callback(false, response.pushToken, response.expireTimestamp,response.baseResponse.errMsg)
            }

        }
        if (!result) {
            callback(false, "", -1,"fail")
        }
    }

    fun postMsg(accessToken: String, token: String, msg: String, delay: Int, callback: (Boolean, String) -> Unit) {
        Thread {
            Log.i(TAG, "postMsg: push [${msg}]")
            val mediaType = MediaType.parse("text/plain")
            val body = RequestBody.create(mediaType, "{\n   \"push_token\":\"$token\",\n   \"msg\":\"$msg\"\n}")
            val request = Request.Builder()
                    .url("https://api.weixin.qq.com/wxa/business/runtime/appmsg/push?access_token=$accessToken")
                    .post(body)
                    .addHeader("Content-Type", "text/plain")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Host", "api.weixin.qq.com")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
                    .build()
            try {
                val response = client.newCall(request).execute()
                callback(response.isSuccessful, response.toString())
            } catch (e: Exception) {

            }
        }.start()
    }

    fun setMsgCallback(ui: PushMsgQuickStartActivity) {
        Thread {
            val request = WMPFPushMsgRequest()
            request.baseRequest = WMPFBaseRequestHelper.checked()
            WMPFIPCInvoker.invokeAsync<IPCInovkerTask_SetPushMsgCallback,
                    WMPFPushMsgRequest, WMPFPushMsgResponse>(request, IPCInovkerTask_SetPushMsgCallback::class.java) { resp ->
                ui.printlnToView("receive: " + resp.msgBody)
            }
            Response(true, "")
        }.start()
    }
}

data class Response<out A, B>(
        val isSuccess: A,
        var body: B
) {
    override fun toString(): String = "($isSuccess, $body)"
}
