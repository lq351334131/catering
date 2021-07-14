package com.tencent.wmpf.pos.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.sunmi.devicemanager.cons.Cons
import com.sunmi.devicesdk.core.PrinterManager
import com.sunmi.extprinterservice.ExtPrinterService
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.tencent.wmpf.pos.BuildConfig
import com.tencent.wmpf.pos.MyApplication
import com.tencent.wmpf.pos.R
import com.tencent.wmpf.pos.sunmi.present.KPrinterPresenter
import com.tencent.wmpf.pos.sunmi.present.PrinterPresenter
import com.tencent.wmpf.pos.sunmi.utils.Constants
import com.tencent.wmpf.pos.sunmi.utils.ThreadPoolManager
import com.tencent.wmpf.pos.utils.*
import com.tencent.wmpf.pos.widget.DownloadProgressDialog
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class TestActivity : AppCompatActivity() {

    var printerPresenter: PrinterPresenter? = null
    var kPrinterPresenter: KPrinterPresenter? = null
    var isK1 = false
    private var woyouService: SunmiPrinterService? = null//商米标准打印 打印服务
    private var extPrinterService: ExtPrinterService? = null//k1 打印服务

    var updateProgressDialog: DownloadProgressDialog? = null

    private var edt: EditText? = null
    private var download: Button? = null
    private var print: Button? = null
    private var downloadPic: Button? = null
    private var print_out: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val dm = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(dm)
        val width = dm.widthPixels// 屏幕宽度
        val height = dm.heightPixels// 屏幕宽度
        isVertical = height > width

        isK1 = MyApplication.getInstance().isHaveCamera && isVertical

        if (isK1) {
            connectKPrintService()
        } else {
            connectPrintService()
        }

        edt = findViewById(R.id.edit_pay_code)
        download = findViewById(R.id.download)
        print = findViewById(R.id.print)
        downloadPic = findViewById(R.id.download_pic)
        print_out = findViewById(R.id.print_out)

        edt!!.setOnEditorActionListener { v, _, _ ->
            val str = v.text.toString().trim { it <= ' ' }
            Log.e("付款码：", str + "")
            if (str.length == 18) {
                v.text = str
            }
            true
        }

        download!!.setOnClickListener{
            downServiceApk("https://github.com/wmpf/wmpf_demo_external/releases/download/v1.0.5/wmpf-arm-production-release-v1.0.5-640-signed.apk")
            /*val info = MyApplication.getInstance().usbInfo
            findViewById<TextView>(R.id.usbInfo).text = info*/
        }

        downloadPic!!.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                } else {
                    FileUtil.downLoadPic("https://ka-sit.etocdn.cn/711/heshenghui(2).jpg", "heshenghui(2).jpg")
                }
            }
        }

        print!!.setOnClickListener{
            val data = "{\"title\":\"Sunmi 收银演示程序\",\"head\":{\"param1\":\"编号\",\"param2\":\"品名\",\"param3\":\"单价\"},\"flag\":\"true\",\"list\":[{\"param1\":\"1\",\"param2\":\"信远斋桂花酸梅汤\",\"param3\":\"¥7.0\",\"type\":0,\"code\":\"6905069998814\",\"net\":0}],\"KVPList\":[{\"name\":\"总计 \",\"value\":\"7.00\"},{\"name\":\"优惠 \",\"value\":\"0.00\"},{\"name\":\"数量 \",\"value\":\"1\"},{\"name\":\"应收 \",\"value\":\"7.00\"}]}"
            //paySuccessToPrinter(data, Constants.PAY_MODE_0)
            val jsonData = FileUtil.getFromAssets(this, "json3.txt")
            jsonPrint(jsonData)
        }
        print_out!!.setOnClickListener{
            startActivity(Intent(this, PrintTestActivity::class.java))
        }
    }

    //读取文件
    private fun getFromAssets(fileName: String): String {
        try {
            val inputReader = InputStreamReader(
                resources.assets.open(fileName))
            val bufReader = BufferedReader(inputReader)
            var line = ""
            var result = ""
            while (bufReader.readLine() != null) {
                line = bufReader.readLine()
                result += line
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun requestEditText() {
        if (edt != null) {
            edt!!.isFocusable = true
            edt!!.isFocusableInTouchMode = true
            edt!!.requestFocus()
        }
    }

    private var downloadTask : AsyncTask<String, Int, Unit>? = null

    //下载wmpf服务应用
    private fun downServiceApk(apkUrl: String) {
        //判断是否需要下载，如果不需要则return
        /* if (needDownLoad) {
             return
         }*/
        updateProgressDialog = DownloadProgressDialog()
        updateProgressDialog?.isCancelable = false             //强制更新弹窗不消失
        downloadTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<String, Int, Unit>(){

            val dir = File(SDCardFileUtils.creatDir2SDCard(SDCardFileUtils.CREATE_CARD_PATH))
            val file = File(dir, "wmpfService.apk")

            override fun doInBackground(vararg params: String?) {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    var fileOutputStream: FileOutputStream? = null

                    try {
                        var count = 0
                        val url = URL(apkUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        val isInput = conn.inputStream
                        updateProgressDialog?.setTotal(conn.contentLength)
                        if (isInput != null) {
                            if (file.exists()) {
                                file.delete()
                            }
                            fileOutputStream = FileOutputStream(file)
                            val buf = ByteArray(4096)
                            var temp: Int
                            fileOutputStream.let { it ->
                                while (isInput.read(buf).also { temp = it } != -1) {
                                    it.write(buf, 0, temp)
                                    count += temp
                                    publishProgress(count)
                                }
                            }
                        }
                        fileOutputStream?.flush()
                        fileOutputStream?.close()

                    } catch (e: MalformedURLException) {
                        Looper.prepare()
                        ToastUtil.showToast("更新失败，请重新尝试")
                        finish()
                        e.printStackTrace()
                    } catch (e: IOException) {
                        Looper.prepare()
                        ToastUtil.showToast("更新失败，请重新尝试")
                        finish()
                        e.printStackTrace()
                    } finally {
                        fileOutputStream?.close()
                    }

                }
                return
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)
                if (file.exists()) {
                    installApk(this@TestActivity, Environment.getExternalStorageDirectory().path + "/wmpf/wmpfService.apk")
                }
                finish()
            }

            override fun onProgressUpdate(vararg values: Int?) {
                super.onProgressUpdate(*values)
                values[0]?.let {
                    updateProgressDialog?.updateProgress(it)
                }

            }
        }
        updateProgressDialog?.setDismissCallBack { downloadTask?.cancel(false) }
        updateProgressDialog?.setUpdateStartCallBack {
            //申请手机存储权限·
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 检查权限状态
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //  用户未彻底拒绝授予权限用户彻底拒绝授予权限，一般会提示用户进入设置权限界面
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
                    } else {
                        ToastUtil.showToast("请打开该应用存储权限")
                    }
                } else {
                    updateProgressDialog?.beginDownload()
                    downloadTask?.execute()
                }
            }
        }
        //DialogFragmentManager.obtain().addDialog(BaseDialogFragmentWrapper(updateProgressDialog, "updateApp", true))
        updateProgressDialog?.show(supportFragmentManager, "app_update", true)
    }

    // 安装apk
    fun installApk(context: Context?, apkPath: String) {
        if (context == null || TextUtils.isEmpty(apkPath)) {
            return
        }
        val file = File(apkPath)
        val intent = Intent(Intent.ACTION_VIEW)

        //判断版本是否在7.0以上
        if (Build.VERSION.SDK_INT >= 24) {
            //provider authorities
            val apkUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+ ".fileprovider", file)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
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
            printerPresenter = PrinterPresenter(this@TestActivity, woyouService)

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
            kPrinterPresenter = KPrinterPresenter(this@TestActivity, extPrinterService)
        }
    }

    fun jsonPrint(data: String) {
        val printer = PrinterUtils.getPrinter()
        printer.parse(data)
        paySuccessToPrinter(printer.toJson().toString(), Constants.PAY_MODE_0)
    }

    //打印
    private fun paySuccessToPrinter(printData: String, payMode: Int) {
        if (isK1) run {
            if (kPrinterPresenter != null) {
                kPrinterPresenter!!.print(printData, payMode)
            }
        } else {
            if (printerPresenter == null) {
                printerPresenter = PrinterPresenter(this@TestActivity, woyouService)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            for (i in permissions.indices) {
                if (grantResults[i] == PermissionChecker.PERMISSION_GRANTED) {
                    FileUtil.downLoadPic("https://ka-sit.etocdn.cn/711/heshenghui(2).jpg", "heshenghui(2).jpg")
                } else {
                    ToastUtil.showToast("请打开该应用存储权限")
                }
            }
        }
    }

    companion object {
        @JvmField
        var isVertical: Boolean = false
    }

    override fun onDestroy() {
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
        super.onDestroy()
    }
}
