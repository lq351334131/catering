@file:Suppress("SpellCheckingInspection", "SpellCheckingInspection", "SpellCheckingInspection", "SpellCheckingInspection")

package com.tencent.luggage.demo.wxapi

import android.util.Log
import com.tencent.mmkv.MMKV
import com.tencent.wmpf.pos.BuildConfig
import com.tencent.wmpf.pos.sunmi.utils.Utils

object DeviceInfo {
    /**
     * NOTE:
     * WARNING: You should never have your app secret stored in client.
     * The following code is a MISTAKE.
     * temp here!
     */
    const val APP_ID = BuildConfig.HOST_APPID // com.tencent.luggage.demo
    const val APP_SECRET = BuildConfig.HOST_APPSECRET
    private const val DEFAULT_EXPIRED_TIME_MS = -1L

    private const val TAG = "Constants"
    private val kv = MMKV.mmkvWithID(TAG, MMKV.SINGLE_PROCESS_MODE)

    const val KEY_TEST_PRODUCT_ID = "product_id"

    const val KEY_TEST_DEVICE_ID = "device_id"

    const val KEY_TEST_SIGNATURE = "signature"

    const val KEY_TEST_KEY_VERSION = "key_version"

    const val KEY_EXPIRED_TIME_MS = "expiredTimeMs"

    /**
     * NOTICE HERE!!!
     * set to ture if you want to user_red your own device info
     */
    const val isInProductionEnv = true

    var expiredTimeMs = DEFAULT_EXPIRED_TIME_MS
        get() = kv.getLong(KEY_EXPIRED_TIME_MS, DEFAULT_EXPIRED_TIME_MS)
        set(value) {
            kv.putLong(KEY_EXPIRED_TIME_MS, value)
            field = value
        }

    var productId: Int = 0
        get() {
            return if (isExpired() || isInProductionEnv) {
                969 // REPLACE YOUR OWN DEVICE INFO
            } else {
                kv.getInt(KEY_TEST_PRODUCT_ID, 0)
            }
        }
        set(value) {
            kv.putInt(KEY_TEST_PRODUCT_ID, value)
            field = value
        }

    var keyVersion: Int = 0
        get() {
            return if (isExpired() || isInProductionEnv) {
                1 // REPLACE YOUR OWN DEVICE INFO
                //kv.getInt(KEY_TEST_KEY_VERSION, 0)
            } else {
                kv.getInt(KEY_TEST_KEY_VERSION, 0)
            }
        }
        set(value) {
            kv.putInt(KEY_TEST_KEY_VERSION, value)
            field = value
        }

    var deviceId: String = ""
        get() {
            //T229206V40094
            //DA04189K70863
            return if (isExpired() || isInProductionEnv) {
                //"T229206V40094" // REPLACE YOUR OWN DEVICE INFO
                Utils.getSN()
            } else {
                kv.getString(KEY_TEST_DEVICE_ID, "")!!
            }
        }
        set(value) {
            kv.putString(KEY_TEST_DEVICE_ID, value)
            field = value
        }

    var signature: String = ""
        get() {
            //T2
            //MEQCICUlF0n4iHUrqZJO43h+z8FCr0ahOC/nktAQSGCShZcqAiAze10pnW8u71icbA6O62ZzCAM7waer4k8BucxtWtfihw==
            //D2
            //MEUCIHsQYQVg3BrAE/YzMmKfJ5anlNzPlnI5b+o9JRyszZYZAiEAhrcnV3WKvlyU8b/kcBhj7g8aU99nWSpSOAcF99k+K1A=
            return if (isExpired() || isInProductionEnv) {
                //"MEQCICUlF0n4iHUrqZJO43h+z8FCr0ahOC/nktAQSGCShZcqAiAze10pnW8u71icbA6O62ZzCAM7waer4k8BucxtWtfihw==" // REPLACE YOUR OWN DEVICE INFO
                kv.getString(KEY_TEST_SIGNATURE, "")!!
            } else {
                kv.getString(KEY_TEST_SIGNATURE, "")!!
            }
        }
        set(value) {
            kv.putString(KEY_TEST_SIGNATURE, value)
            field = value
        }

    fun isInited(): Boolean {
        return expiredTimeMs != DEFAULT_EXPIRED_TIME_MS
    }

    fun isExpired(): Boolean {
        val ret = expiredTimeMs > System.currentTimeMillis()
        if (ret) Log.e(TAG, "isExpired: deviceInfo is isExpired or not init")
        return ret
    }

    fun reset() {
        expiredTimeMs = -1
        productId = 0
        deviceId = ""
        signature = ""
        keyVersion = 0
    }
}