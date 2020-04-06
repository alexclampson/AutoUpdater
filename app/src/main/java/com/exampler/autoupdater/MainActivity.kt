package com.exampler.autoupdater

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.exampler.autoupdater.api.DownloadController
import kotlinx.android.synthetic.main.activity_main.*

const val TAG = "Mylogs"
const val BASE_URL = "http://apps.tw1.ru"
const val LATEST_VERSION_URL = "/testapp/latest_version.txt"
const val LATEST_VERSION_APK = "/testapp/app-debug.apk"

class MainActivity : AppCompatActivity() {
    lateinit var downloadController: DownloadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val manager: PackageManager = this.packageManager
        val info: PackageInfo = manager.getPackageInfo(this.packageName, 0)
        val currentVersion = info.versionName
        tv_version.text = currentVersion

        downloadController = DownloadController(tv_logUpdate, this)
        downloadController.checkVersion()

    }
}
