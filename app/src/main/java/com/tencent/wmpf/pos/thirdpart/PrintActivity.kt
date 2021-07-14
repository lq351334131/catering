package com.tencent.wmpf.pos.thirdpart

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.widget.Toast
import com.bumptech.glide.Glide
import com.sunmi.devicemanager.cons.Cons
import com.sunmi.devicesdk.core.PrinterManager
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.sunmi.extprinterservice.ExtPrinterService
import com.sunmi.peripheral.printer.*
import com.tencent.mm.opensdk.utils.Log
import com.tencent.wmpf.event.CallbackEvent
import com.tencent.wmpf.event.PrintEvent
import com.tencent.wmpf.pos.BuildConfig
import com.tencent.wmpf.pos.MyApplication
import com.tencent.wmpf.pos.R
import com.tencent.wmpf.pos.RequestsRepo
import com.tencent.wmpf.pos.sunmi.present.KPrinterPresenter
import com.tencent.wmpf.pos.sunmi.present.PrinterPresenter
import com.tencent.wmpf.pos.sunmi.utils.ThreadPoolManager
import com.tencent.wmpf.pos.sunmi.utils.Utils
import com.tencent.wmpf.pos.utils.*
import com.tencent.wmpf.pos.sunmi.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_third_part_api_demo.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PrintActivity : AppCompatActivity() {
    var printerPresenter: PrinterPresenter? = null
    var kPrinterPresenter: KPrinterPresenter? = null
    var isK1 = false
    private var woyouService: SunmiPrinterService? = null//商米标准打印 打印服务
    private var extPrinterService: ExtPrinterService? = null//k1 打印服务
    private var rawData:String? = null
    private var isConnected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_part_api_demo)

        Glide.with(this).asGif().load(R.mipmap.print).into(iv_print)
        EventBus.getDefault().register(this)
        if (intent?.action.equals(ThirdpartConstants.Printer.Action.ACTION_THIRDPART)) {
            val appId = BuildConfig.MAIN_APPID
            val timeStamp = intent.extras?.getLong(ThirdpartConstants.Printer.Key.KEY_TIME_STAMP)
            val token = intent.extras?.getString(ThirdpartConstants.Printer.Key.KEY_TOKEN)
            val tokenLocalGen= MD5Util.getMD5String(appId + "_" + timeStamp)
            if (tokenLocalGen != token) {
                Log.e(TAG, "token invalid")
                Toast.makeText(applicationContext, "printer: token invalid!", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
            // rawData为前端透传过来的自定义数据结构字符串，对应前端data的数据
            rawData = intent.extras?.getString(ThirdpartConstants.Printer.Key.KEY_RAW_DATA)

            val dm = DisplayMetrics()
            window.windowManager.defaultDisplay.getMetrics(dm)
            val width = dm.widthPixels// 屏幕宽度
            val height = dm.heightPixels// 屏幕高度
            isVertical = height > width

            isK1 = MyApplication.getInstance().isHaveCamera && isVertical

            if (isK1) {
                connectKPrintService()
            } else {
                connectPrintService()
            }
            getImage()
        }
    }

    private fun getImage() {
        RequestsRepo.getImage(Utils.getSN()) { success, list ->
            if (success) {
                list?.forEach {
                    val imageName = it.substring(it.lastIndexOf("/") + 1)
                    val dir = Environment.getExternalStorageDirectory().absolutePath + "/pos/"
                    val file = File(dir, imageName)
                    if (!file.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                            } else {
                                FileUtil.downLoadPic(it, imageName)
                            }
                        }
                    }
                }
                jsonPrint(rawData!!)
            } else {
                jsonPrint(rawData!!)
            }
        }
    }

    //连接打印服务
    private fun connectPrintService() {
        try {
            InnerPrinterManager.getInstance().bindService(this,
                    innerPrinterCallback)
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
    }

    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            woyouService = service
            printerPresenter = PrinterPresenter(this@PrintActivity, woyouService)
        }

        override fun onDisconnected() {
            woyouService = null

        }
    }

    //连接K1打印服务
    private fun connectKPrintService() {
        val intent = Intent()
        intent.setPackage("com.sunmi.extprinterservice")
        intent.action = "com.sunmi.extprinterservice.PrinterService"
        bindService(intent, connService, Context.BIND_AUTO_CREATE)
    }

    private val connService = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            extPrinterService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            extPrinterService = ExtPrinterService.Stub.asInterface(service)
            kPrinterPresenter = KPrinterPresenter(this@PrintActivity, extPrinterService)
        }
    }

    private fun jsonPrint(data: String) {
        val printer = PrinterUtils.getPrinter()
        printer.parse(data)
        //paySuccessToPrinter(printer.toJson().toString(), Constants.PAY_MODE_0)
        connectPrinter(printer.toJson().toString())
    }

    private fun connectPrinter(data: String) {
        if (!isConnected) {
            SunmiPrinterApi.getInstance().connectPrinter(this, SunmiPrinter.SunmiNTPrinter,
                    object : ConnectCallback {
                        override fun onFound() {
                            //发现打印机会回调此⽅法
                            LogUtil.log(TAG, "找到了打印机")
                        }

                        override fun onUnfound() {
                            //如果没找到打印机会回调此⽅法
                            runOnUiThread{
                                ToastUtil.showToast("未找到打印机")
                            }
                            finish()
                        }

                        override fun onConnect() {
                            //连接成功后会回调此⽅法，则可以打印
                            LogUtil.log(TAG, "打印机已经连接")
                            toPrint(data)
                        }

                        override fun onDisconnect() {
                            //连接中打印机断开会回调此⽅法，此时将中断打印
                            runOnUiThread{
                                ToastUtil.showToast("打印机已断开连接")
                            }
                            finish()
                        }
                    }
            )
        } else {
            toPrint(data)
        }
    }

    private fun toPrint(data: String) {
        val sunmiPrinterApi = SunmiPrinterApi.getInstance()
        if (sunmiPrinterApi.printerStatus != 0) {
            return
        }
        val `object` = JSONObject(data)
        val array: JSONArray
        if (`object`.optJSONArray("spos") != null) {
            array = `object`.optJSONArray("spos")
            for (i in 0 until array.length()) {
                val data = array.opt(i) as JSONObject
                LogUtil.log("printData", data.toString())
                sunmiPrinterApi.enableBold(false)
                sunmiPrinterApi.setAlignMode(0)
                when (data.optString("contenttype")) {
                    "txt" -> {
                        //设置粗体
                        if (data.optInt("bold") == 1) {
                            sunmiPrinterApi.enableBold(true)
                        }
                        sunmiPrinterApi.setAlignMode(data.optInt("position"))
                        //sunmiPrinterApi.setFontZoom(data.optInt("hori"), data.optInt("veri"))
                        sunmiPrinterApi.printText(data.optString("content") + "\n")
                    }
                    "line" -> {
                        sunmiPrinterApi.setAlignMode(data.optInt("position"))
                        sunmiPrinterApi.printText("--------------------------------\n")
                    }
                    "bmp" -> {
                        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.print_logo);
                        var bitmap: Bitmap? = BitmapFactory.decodeFile(FileUtil.picPath + data.optString("content"))
                        val pixel = 384
                        if (bitmap != null) {
                            if (bitmap.width > pixel) {
                                val newHeight = (1.0 * bitmap.height.toDouble() * pixel.toDouble() / bitmap.width).toInt()
                                bitmap = BitmapUtils.scale(bitmap, pixel, newHeight)
                            }
                            sunmiPrinterApi.setAlignMode(data.optInt("position"))
                            sunmiPrinterApi.printBitmap(bitmap!!, 0)
                            sunmiPrinterApi.lineWrap(2)
                        }
                    }
                    "one-dimension" -> {
                        sunmiPrinterApi.setAlignMode(data.optInt("position"))
                        sunmiPrinterApi.printBarCode(data.optString("content"), 8, data.optInt("height"), data.optInt("width"), data.optInt("position"))
                        sunmiPrinterApi.printText("\n")
                    }
                    "two-dimension" -> {
                        sunmiPrinterApi.setAlignMode(data.optInt("position"))
                        sunmiPrinterApi.printQrCode(data.optString("content"), data.optInt("qrsize"), 3)
                        sunmiPrinterApi.printText("\n")
                    }
                }
            }
        }
        EventBus.getDefault().post(CallbackEvent())
    }

    override fun onResume() {
        super.onResume()
        isConnected = SunmiPrinterApi.getInstance().isConnected
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: CallbackEvent) {
        val intent = Intent().apply {
            // 将执行结果通知到wmpf，否则前端接受到的回调参数会错误
            putExtra(ThirdpartConstants.Printer.Key.KEY_RESULT_CODE, ThirdpartConstants.Printer.Code.CODE_SUCCESS)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: PrintEvent) {
        jsonPrint(rawData!!)
    }

    //打印
    private fun paySuccessToPrinter(printData: String, payMode: Int) {
        if (isK1) run {
            if (kPrinterPresenter != null) {
                //kPrinterPresenter!!.print(printData, payMode)
                ToastUtil.showToast("暂不支持此设备")
            }
        } else {
            if (printerPresenter == null) {
                printerPresenter = PrinterPresenter(this@PrintActivity, woyouService)
            }
            printerPresenter!!.print2(printData, payMode)
            ThreadPoolManager.getsInstance().execute {
                val deviceList = PrinterManager.getInstance().printerDevice
                if (deviceList == null || deviceList.isEmpty()) return@execute
                for (device in deviceList) {
                    if (device.type == Cons.Type.PRINT && device.connectType == Cons.ConT.INNER) {
                        continue
                    }
                    printerPresenter!!.printByDeviceManager(printData, payMode, device)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PrintActivity"
        @JvmField
        var isVertical: Boolean = false
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        if (isConnected) {
            SunmiPrinterApi.getInstance().disconnectPrinter(this)
        }
        if (extPrinterService != null) {
            unbindService(connService)
        }
        if (woyouService != null) {
            try {
                InnerPrinterManager.getInstance().unBindService(this,
                        innerPrinterCallback)
            } catch (e: InnerPrinterException) {
                e.printStackTrace()
            }

        }

        printerPresenter = null
        kPrinterPresenter = null
    }
}