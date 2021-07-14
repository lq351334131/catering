package com.tencent.wmpf.pos.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import com.tencent.luggage.demo.wxapi.DeviceInfo
import com.tencent.wmpf.pos.*
import com.tencent.wmpf.pos.listeners.NoDoubleClickListener
import com.tencent.wmpf.pos.sunmi.utils.Constants
import com.tencent.wmpf.pos.sunmi.utils.SharePreferenceUtil
import com.tencent.wmpf.pos.sunmi.utils.Utils
import com.tencent.wmpf.pos.utils.AndroidUtils
import com.tencent.wmpf.pos.utils.LogUtil
import com.tencent.wmpf.pos.utils.ToastUtil
import kotlinx.android.synthetic.guide.activity_activate.*

class ActivateActivity : BaseActivity() {

    private var appType = 0
    private var appId = BuildConfig.MAIN_APPID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activate)

        if (BuildConfig.DEBUG) {
            appId = BuildConfig.TEST_APPID
            appType = 2
        }
        AndroidUtils.isButtonEnable(intArrayOf(1, 1), activate, org_id, store_id)
        activate.setOnClickListener(object : NoDoubleClickListener() {
            override fun onNoDoubleClick(view: View?) {
                val token = SharePreferenceUtil.getParam(MyApplication.app, Constants.TOKEN, "")
                RequestsRepo.getSign(token.toString(), Utils.getSN(), org_id.text.toString(), store_id.text.toString(), DeviceInfo.keyVersion, "cash_pos", android.os.Build.BRAND + android.os.Build.MODEL) { success, resp ->
                    if (success) {
                        LogUtil.log(TAG, resp)
                        view!!.post {
                            ToastUtil.showToast("激活成功")
                        }
                        DeviceInfo.signature = resp
                        launchWxa()
                        finish()
                    } else {
                        view!!.post {
                            ToastUtil.showToast(resp)
                        }
                    }
                }
            }
        })
    }

    //校验能否激活成功
    @SuppressLint("CheckResult")
    fun activate(signature: String, keyVersion: Int) {
        Api.activateDevice(DeviceInfo.productId, DeviceInfo.keyVersion, DeviceInfo.deviceId, DeviceInfo.signature, DeviceInfo.APP_ID)
            .doOnSuccess {
                ToastUtil.showToast("激活成功")
                DeviceInfo.signature = signature
                DeviceInfo.keyVersion = keyVersion
                finish() }
            .doOnError {
                ToastUtil.showToast("激活失败，请检查公钥版本号是否正确")
                activate.isEnabled = true
            }
            .subscribe({
                Log.e(TAG, "success: $it")
            },{
                Log.e(TAG, "error: $it")
            })
    }

    @SuppressLint("CheckResult")
    private fun launchWxa() {
        Api.activateDevice(DeviceInfo.productId, DeviceInfo.keyVersion, DeviceInfo.deviceId, DeviceInfo.signature, DeviceInfo.APP_ID)
            .flatMap {
                Api.launchWxaApp(appId, "", 0, appType)
            }
            .subscribe({
                Log.e(TAG, "success: $it")
            }, {
                Log.e(TAG, "error: $it")
            })
    }

    companion object {
        private const val TAG = "ActivateActivity"
    }
}
