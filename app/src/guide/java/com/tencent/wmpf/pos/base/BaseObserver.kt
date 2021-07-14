package com.tencent.wmpf.pos.base

import android.accounts.NetworkErrorException
import android.content.Context

import com.google.gson.JsonSyntaxException
import com.tencent.wmpf.pos.BuildConfig
import com.tencent.wmpf.pos.utils.LogUtil
import com.tencent.wmpf.pos.utils.ToastUtil

import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.lang.Exception

/**
 * Created by gaochujia on 2020-12-25.
 */

abstract class BaseObserver<T>(private val mContext: Context?) : Observer<BaseNetworkResponse<T>> {

    override fun onSubscribe(d: Disposable) {
        onRequestStart()
    }

    override fun onNext(baseNetworkResponse: BaseNetworkResponse<T>) {
        if (!baseNetworkResponse.isError) {
            try {
                onSuccess(baseNetworkResponse.data, baseNetworkResponse.msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            try {
                if (baseNetworkResponse.code == 1001) {
                    //EventBus.getDefault().post(LoginInvalidEvent())
                }
                onCodeError(baseNetworkResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onError(e: Throwable) {
        if (null != e.message) {
            LogUtil.log("error", "onError: " + e.message)
        }
        try {
            if (e is ConnectException
                || e is TimeoutException
                || e is NetworkErrorException
                || e is UnknownHostException
            ) {
                if (null != mContext) {
                    ToastUtil.showToast("网络异常，请稍后重试")
                }
            }
            if (e is JsonSyntaxException) {
                if (BuildConfig.DEBUG) {
                    ToastUtil.showToast("Gson解析出错")
                }
            }
            onFailure(e)
        } catch (e1: Exception) {
            e1.printStackTrace()
        } finally {
            onFinally()
        }
    }

    override fun onComplete() {
        onFinally()
    }

    /**
     * 返回成功
     *
     * @param data
     * @throws Exception
     */
    protected abstract fun onSuccess(data: T?, msg: String?)

    /**
     * 返回成功了,但是code错误
     *
     * @param t
     * @throws Exception
     */
    @Throws(Exception::class)
    protected abstract fun onCodeError(t: BaseNetworkResponse<T>)

    /**
     * 返回失败
     *
     * @param e
     * @throws Exception
     */
    @Throws(Exception::class)
    protected abstract fun onFailure(e: Throwable)
    protected abstract fun onRequestStart()
    protected abstract fun onFinally()
}
