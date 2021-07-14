package com.tencent.wmpf.pos.utils

import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.tencent.wmpf.pos.MyApplication


/**
 * Created by gaochujia on 2020-12-22.
 */

object ToastUtil {

    private var mToast: Toast? = null
    private var centerToast: Toast? = null
    private val mHandler = Handler()
    private val toastRunnable = Runnable {
        mToast!!.cancel()
        mToast = null//toast隐藏后，将其置为null
    }

    /**
     * 在底部显示提示信息
     *
     * @param msg 需要显示的信息，如果为空的话，
     * 那么显示"网络出错，请稍后再试"
     */
    fun showToast(msg: String?) {

        //cancelToast()
        if (mToast == null)
        //只有mToast==null时才重新创建，否则只需更改提示文字
        {
            mToast = Toast.makeText(MyApplication.getInstance().applicationContext, "", Toast.LENGTH_SHORT)
        }
        mToast!!.setText(msg)
        // mHandler.postDelayed(toastRunnable, 2000)//延迟1秒隐藏toast
        mToast!!.show()
    }

    /**
     * 显示（时间是短暂的）吐司消息居中
     *
     * @param context 上下文
     * @param msg     消息内容
     */
    fun showToastInCenter(msg: String?) {
        try {
            if (centerToast == null) {
                centerToast = Toast.makeText(MyApplication.app.applicationContext, msg, Toast.LENGTH_SHORT)
                centerToast!!.setGravity(Gravity.CENTER, 0, 0)
            }
            centerToast!!.setText(msg)
            centerToast!!.show()
        } catch (e: Exception) {
            Log.e("toast", e.toString())
        }

    }

    fun cancelToast() {
        mHandler.removeCallbacks(toastRunnable)
        if (mToast != null) {
            mToast!!.cancel()
        }
    }
}
