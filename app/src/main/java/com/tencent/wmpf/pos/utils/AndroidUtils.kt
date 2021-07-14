package com.tencent.wmpf.pos.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText

/**
 * Created by gary.gao on 2021/5/25
 */
object AndroidUtils {
    /**
     * 通过指定树龄的EditText的长度来控制任意view的enable状态
     * 主要用于登录注册页面的输入框判断
     * TODO 目前暂未实现对最大长度的控制
     *
     * @param minLengthAraay EditText对应的最小长度
     * @param enableView     需要控制的view
     * @param ets            对应的EditText
     */
    fun isButtonEnable(minLengthAraay: IntArray?, enableView: View, vararg ets: EditText) {
        if (minLengthAraay == null || ets == null) return //有空值，直接结束
        if (minLengthAraay.size != ets.size) return //数量不匹配，直接结束
        for (i in ets.indices) {
            if (ets == null || minLengthAraay[i] < 0) return //传入值非法，直接结束
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                var enable = true
                for (j in ets.indices) {
                    if (ets[j].text.length < minLengthAraay[j]) {//如果大于表示匹配成功
                        enable = false
                    }
                }
                enableView.isEnabled = enable
            }

            override fun afterTextChanged(editable: Editable) {

            }
        }

        //以此3设置监听器
        for (i in ets.indices) {
            ets[i].addTextChangedListener(watcher)
        }

    }
}