package com.tencent.wmpf.pos.base

/**
 * Created by gaochujia on 2020-12-25.
 */

class BaseNetworkResponse<T> : BaseEntity() {
    var isError: Boolean = false
        get() = 0 != code
    var data: T? = null
    val code: Int = 0
    var msg: String? = null
}
