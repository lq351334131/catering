package com.tencent.wmpf.pos.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tencent.wmpf.pos.MyApplication
import com.tencent.wmpf.pos.R
import com.tencent.wmpf.pos.RequestsRepo
import com.tencent.wmpf.pos.bean.User
import com.tencent.wmpf.pos.listeners.NoDoubleClickListener
import com.tencent.wmpf.pos.sunmi.utils.Constants
import com.tencent.wmpf.pos.sunmi.utils.SharePreferenceUtil
import com.tencent.wmpf.pos.utils.AndroidUtils
import com.tencent.wmpf.pos.utils.LogUtil
import com.tencent.wmpf.pos.utils.MD5Util
import com.tencent.wmpf.pos.utils.ToastUtil
import kotlinx.android.synthetic.guide.activity_login.*

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AndroidUtils.isButtonEnable(intArrayOf(1, 1), login, user_name, password)

        login.setOnClickListener(object : NoDoubleClickListener() {
            override fun onNoDoubleClick(view: View?) {
                val user = User()
                user.userName = user_name.text.toString()
                user.password = MD5Util.getMD5String(password.text.toString())
                RequestsRepo.getAppToken(user) { success, resp ->
                    if (success) {
                        LogUtil.log(TAG, resp)
                        SharePreferenceUtil.setParam(MyApplication.app, Constants.TOKEN, resp)
                        startActivity(Intent(this@LoginActivity, ActivateActivity::class.java))
                        view!!.post {
                            ToastUtil.showToast("登录成功")
                        }
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

    companion object {
        private const val TAG = "LoginActivity"
    }
}
