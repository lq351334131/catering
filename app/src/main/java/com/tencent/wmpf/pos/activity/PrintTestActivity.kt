package com.tencent.wmpf.pos.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.tencent.wmpf.pos.R
import com.tencent.wmpf.pos.utils.ToastUtil

class PrintTestActivity : AppCompatActivity() {

    private var print: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)
        print = findViewById(R.id.print)
        print!!.setOnClickListener {
            connectPrint()
        }
    }

    private fun connectPrint() {
        //打印机连接状态
        //SunmiPrinterApi.getInstance().isConnected
        //获取打印机状态
        //SunmiPrinterApi.getInstance().printerStatus
        //当不再使⽤打印机时，尽可能断开打印机连接，这样将释放资源
        //SunmiPrinterApi.getInstance().disconnectPrinter(this)
        SunmiPrinterApi.getInstance().connectPrinter(this, SunmiPrinter.SunmiNTPrinter,
            object: ConnectCallback {
                override fun onFound() {
                    //发现打印机会回调此⽅法
                    ToastUtil.showToast("找到了打印机")
                }
                override fun onUnfound() {
                    //如果没找到打印机会回调此⽅法
                    ToastUtil.showToast("未找到打印机")
                }
                override fun onConnect() {
                    //连接成功后会回调此⽅法，则可以打印
                    ToastUtil.showToast("打印机已经连接")
                    val sunmiPrinterApi = SunmiPrinterApi.getInstance()
                    sunmiPrinterApi.setFontZoom(1, 1)
                    sunmiPrinterApi.printText("默认字体大小默认字体大小默认字体大小默认字体大小默认字体大小默认字体大小\n")
                    sunmiPrinterApi.printText("abcdefgabcdefgabcdefgabcdefgabcdefgabcdefgabcdefgabcdefgabcdefg\n")

                }
                override fun onDisconnect() {
                    //连接中打印机断开会回调此⽅法，此时将中断打印
                    ToastUtil.showToast("断开连接")
                }
            }
        )
    }
}
