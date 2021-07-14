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
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import com.sunmi.extprinterservice.ExtPrinterService
import com.sunmi.peripheral.printer.*
import com.tencent.luggage.demo.wxapi.DeviceInfo
import com.tencent.wmpf.event.OpenDrawerEvent
import com.tencent.wmpf.pos.*
import com.tencent.wmpf.pos.sunmi.present.KPrinterPresenter
import com.tencent.wmpf.pos.sunmi.present.PrinterPresenter
import com.tencent.wmpf.pos.sunmi.present.VideoDisplay
import com.tencent.wmpf.pos.sunmi.utils.Constants
import com.tencent.wmpf.pos.sunmi.utils.ScreenManager
import com.tencent.wmpf.pos.sunmi.utils.SharePreferenceUtil
import com.tencent.wmpf.pos.sunmi.utils.Utils
import com.tencent.wmpf.pos.utils.*
import com.tencent.wmpf.pos.widget.DownloadProgressDialog
import kotlinx.android.synthetic.guide.activity_launch.*
import okhttp3.*
import okhttp3.Response
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class LaunchActivity : AppCompatActivity() {

    var updateProgressDialog: DownloadProgressDialog? = null
    private val screenManager = ScreenManager.getInstance()
    var printerPresenter: PrinterPresenter? = null
    var kPrinterPresenter: KPrinterPresenter? = null
    var isK1 = false
    private var woyouService: SunmiPrinterService? = null//商米标准打印 打印服务
    private var extPrinterService: ExtPrinterService? = null//k1 打印服务
    private var videoDisplay: VideoDisplay? = null
    var token = ""
    var image = ""
    var imageName = ""
    private var appType = 0
    private var appId = BuildConfig.MAIN_APPID
    private var url = BuildConfig.WMPF_APK_URL
    private var dir: File? = null
    private var fileName: String? = null
    private var file : File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        EventBus.getDefault().register(this)
        screenManager.init(this)

        dir = File(SDCardFileUtils.creatDir2SDCard(SDCardFileUtils.CREATE_CARD_PATH))
        fileName = url.substring(url.lastIndexOf("/") + 1)
        file = File(dir, fileName!!)
        val display = screenManager.presentationDisplays
        //videoDisplay = VideoDisplay(this, display, Environment.getExternalStorageDirectory().path + "/video_02.mp4")

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

        if (BuildConfig.DEBUG) {
            login.visibility = View.VISIBLE
            tips.visibility = View.VISIBLE
            appId = BuildConfig.TEST_APPID
            appType = 2
        }

        isShowDownload()

        login.setOnClickListener{
            Api.activateDevice(DeviceInfo.productId, DeviceInfo.keyVersion, DeviceInfo.deviceId, DeviceInfo.signature, DeviceInfo.APP_ID)
                .flatMap {
                    Api.authorize()
                }
                .subscribe({
                    Log.e(TAG, "success: ${it.baseResponse.errCode}")
                }, {
                    Log.e(TAG, "error: $it")
                })
        }

        launch_wxa.setOnClickListener{
            isShowDownload()
        }
        getImage()
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
            }
        }
    }

    private fun isShowDownload() {
        if (!FileUtil.checkAppInstalled(this, "com.tencent.wmpf") || !file!!.exists())
            showServiceDownload(url)
        else {
            when {
                TextUtils.isEmpty(DeviceInfo.signature) -> startActivity(Intent(this@LaunchActivity, LoginActivity::class.java))
                else -> launchWxa()
            }
        }
    }

    private var downloadTask : AsyncTask<String, Int, Unit>? = null

    //下载wmpf服务应用
    private fun showServiceDownload(apkUrl: String) {
        updateProgressDialog = DownloadProgressDialog()
        updateProgressDialog?.isCancelable = false             //强制下载弹窗不消失
        updateProgressDialog!!.onCancel { this.finish() }
        downloadTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<String, Int, Unit>(){

            override fun doInBackground(vararg params: String?) {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    var fileOutputStream: FileOutputStream? = null

                    try {
                        var count = 0
                        val url = URL(apkUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.connect()
                        val isInput = conn.inputStream
                        updateProgressDialog?.setTotal(conn.contentLength)
                        if (isInput != null) {
                            if (file!!.exists()) {
                                file!!.delete()
                            }
                            fileOutputStream = FileOutputStream(file!!)
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
                        ToastUtil.showToast("下载失败，请重新尝试")
                        this@LaunchActivity.runOnUiThread {
                            updateProgressDialog!!.reDownload()
                        }
                        e.printStackTrace()
                    } catch (e: Exception) {
                        Looper.prepare()
                        ToastUtil.showToast("下载失败，请重新尝试")
                        this@LaunchActivity.runOnUiThread {
                            updateProgressDialog!!.reDownload()
                        }
                        e.printStackTrace()
                    } finally {
                        fileOutputStream?.close()
                    }

                }
                return
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)
                if (file!!.exists()) {
                    installApk(this@LaunchActivity, Environment.getExternalStorageDirectory().path + "/wmpf/" + fileName)
                }
                updateProgressDialog!!.downloadFinish()
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
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                } else {
                    updateProgressDialog?.beginDownload()
                    downloadTask?.execute()
                }
            }
        }
        updateProgressDialog?.show(supportFragmentManager, "app_update", true)
    }

    //okhttp下载方式，暂时不用
    private fun downloadFile(url: String) {
        updateProgressDialog = DownloadProgressDialog()
        updateProgressDialog?.isCancelable = false             //强制下载弹窗不消失
        updateProgressDialog!!.onCancel { this.finish() }
        val startTime = System.currentTimeMillis()
        LogUtil.log("DOWNLOAD", "startTime=$startTime")
        val okHttpClient = OkHttpClient()

        val request = Request.Builder().url(url).build()
        val call = okHttpClient.newCall(request)
        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 下载失败
                e.printStackTrace()
                LogUtil.log("DOWNLOAD", "download failed")
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                var isInput: InputStream? = null
                val buf = ByteArray(2048)
                var len: Int
                var fos: FileOutputStream? = null
                // 储存下载文件的目录
                val savePath = Environment.getExternalStorageDirectory().absolutePath
                try {
                    isInput = response.body()!!.byteStream()
                    val total = response.body()!!.contentLength()
                    updateProgressDialog?.setTotal(total.toInt())
                    val fileName = url.substring(url.lastIndexOf("/") + 1)
                    val file = File(savePath, fileName)
                    fos = FileOutputStream(file)
                    var sum: Long = 0
                    while (isInput.read(buf).also { len = it } != -1) {
                        fos.write(buf, 0, len)
                        sum += len.toLong()
                        //更新进度条
                        updateProgressDialog?.updateProgress(sum.toInt())
                    }
                    fos.flush()
                    // 下载完成
                    if (file.exists()) {
                        installApk(this@LaunchActivity, Environment.getExternalStorageDirectory().path + fileName)
                    }
                    updateProgressDialog?.downloadFinish()
                    LogUtil.log("DOWNLOAD", "download success")
                    LogUtil.log("DOWNLOAD", "totalTime=" + (System.currentTimeMillis() - startTime))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Looper.prepare()
                    ToastUtil.showToast("下载失败，请重新尝试")
                    this@LaunchActivity.runOnUiThread {
                        updateProgressDialog!!.reDownload()
                    }
                    LogUtil.log("DOWNLOAD", "download failed")
                } finally {
                    isInput?.close()
                    fos?.close()
                }
            }
        }
        updateProgressDialog?.setDismissCallBack { call.cancel() }
        updateProgressDialog?.setUpdateStartCallBack {
            //申请手机存储权限·
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                } else {
                    updateProgressDialog?.beginDownload()
                    call.enqueue(callback)
                }
            }
        }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            for (i in permissions.indices) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    updateProgressDialog?.beginDownload()
                    downloadTask?.execute()
                } else {
                    ToastUtil.showToast("请打开该应用存储权限")
                }
            }
        }
        if (requestCode == 1) {
            for (i in permissions.indices) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    FileUtil.downLoadPic(image, imageName)
                } else {
                    ToastUtil.showToast("请打开该应用存储权限")
                }
            }
        }
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
            printerPresenter = PrinterPresenter(this@LaunchActivity, woyouService)
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
            kPrinterPresenter = KPrinterPresenter(this@LaunchActivity, extPrinterService)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenDrawerEvent) {
        printerPresenter!!.printerService.openDrawer(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        private const val TAG = "LaunchActivity"
        @JvmField
        var isVertical: Boolean = false
    }
}
