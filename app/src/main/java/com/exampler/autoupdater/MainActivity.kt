package com.exampler.autoupdater

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.exampler.autoupdater.api.FileDownloadClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*

const val TAG = "Mylogs"
const val BASE_URL = "http://apps.tw1.ru"
const val LATEST_VERSION_URL = "/testapp/latest_version.txt"
const val LATEST_VERSION_NAME = "latest_version.txt"
const val PERMISSION_REQUEST_STORAGE = 0

class MainActivity : AppCompatActivity() {
    lateinit var downloadController: DownloadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkVersion()



    }

    fun checkVersion(){
        tv_logUpdate.text = "Check updates"
        val builder = Retrofit.Builder()
                .baseUrl(BASE_URL)
        val retrofit = builder.build()

        val fileDownloadClient = retrofit.create(FileDownloadClient::class.java)
        fileDownloadClient.downloadLastVersionInfo()?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                    call: Call<ResponseBody?>?,
                    response: Response<ResponseBody?>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { writeResponseBodyToDisk(it, LATEST_VERSION_NAME) }
                    compareVersion()
                } else {
//                    Toast.makeText(this@MainActivity, "Download error!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Download ERROR!!")
                }


            }

            override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
            ) {
//                Toast.makeText(this@MainActivity, "Download error!", Toast.LENGTH_SHORT).show()
//                Log.d(TAG,"Download ERROR!!")
            }
        })
    }

    fun compareVersion() {
        val latestVersion =
                File(getExternalFilesDir("UPDATE").toString() + File.separator + "latest_version.txt").readText()
        val manager: PackageManager = this.packageManager
        val info: PackageInfo = manager.getPackageInfo(this.packageName, 0)
        val currentVersion = info.versionName
        if (currentVersion.replace(".", "") < latestVersion.replace(".", "")) {
            tv_logUpdate.text = "Downloading updates..."
//            Toast.makeText(this@MainActivity, "New version available ", Toast.LENGTH_SHORT).show()
            val apkUrl = "http://apps.tw1.ru/testapp/app-debug.apk"
            downloadController = DownloadController(this, apkUrl)
            checkStoragePermission()

            downloadController.enqueueDownload()
        } else {
            tv_logUpdate.text = ""
//            Toast.makeText(this@MainActivity, "New version NOT available ", Toast.LENGTH_SHORT)
//                    .show()
        }


    }

    private fun writeResponseBodyToDisk(body: ResponseBody, fileName: String): Boolean {
        return try {
            // todo change the file location/name according to your needs
            val file =
                    File(getExternalFilesDir("UPDATE").toString() + File.separator + fileName)
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(8192)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                while (true) {
                    val read: Int = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                    Log.d(TAG, "file download: $fileSizeDownloaded of $fileSize")
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            return true
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start downloading
//                downloadController.enqueueDownload()
            } else {
                // Permission request was denied.
                mainLayout.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }


    private fun checkStoragePermission() {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            // start downloading
            downloadController.enqueueDownload()
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {

        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mainLayout.showSnackbar(
                    R.string.storage_access_required,
                    Snackbar.LENGTH_INDEFINITE, R.string.ok
            ) {
                requestPermissionsCompat(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_STORAGE
                )
            }

        } else {
            requestPermissionsCompat(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE
            )
        }
    }

}
