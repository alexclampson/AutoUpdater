package com.exampler.autoupdater.api

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.exampler.autoupdater.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*


class DownloadController(private var view: TextView, val context: Context) {



    fun checkVersion() {
        view.text = context.getString(R.string.check_updates)
        val builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
        val retrofit = builder.build()

        val fileDownloadClient = retrofit.create(FileDownloadClient::class.java)
        fileDownloadClient.downloadLastVersionInfo()?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>?, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { writeResponseBodyToDisk(it, "latest_version.txt") }
                    Log.d(TAG, "onResponse: Download latest_version.txt")
                    compareVersion()
                } else {
                    Log.d(TAG, "onResponse:Download latest_version.txt ERROR!!")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.d(TAG, "onFailure:Download latest_version.txt ERROR!!")
            }
        })
    }

    fun compareVersion(){
        val latestVersion =
            File(context.getExternalFilesDir("UPDATE").toString() + File.separator + "latest_version.txt").readText()
        val manager: PackageManager = context.packageManager
        val info: PackageInfo = manager.getPackageInfo(context.packageName, 0)
        val currentVersion = info.versionName
        if (currentVersion.replace(".", "") < latestVersion.replace(".", "")) {
//            downloadApk()
//            MainActivity().runDialog()
            runDialog()

        } else {
            view.text = ""
        }
    }

    private fun runDialog(){
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle(context.resources.getString(R.string.new_version))
        dialog.setMessage(context.resources.getString(R.string.dialog_text))
        dialog.setCancelable(false)
        dialog.setPositiveButton(context.resources.getString(R.string.positive_button)) { _, _ ->
            DownloadController(TextView(context), context).downloadApk(view)
        }
        dialog.setNegativeButton(context.resources.getString(R.string.negative_button)) { _, _ ->
            (context as MainActivity).finish()
        }
        dialog.show()




    }

    private fun downloadApk(view: TextView) {
        view.text = context.resources.getText(R.string.downloading)
        val builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
        val retrofit = builder.build()

        val fileDownloadClient = retrofit.create(FileDownloadClient::class.java)
        fileDownloadClient.downloadLastApk().enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>?, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { writeResponseBodyToDisk(it, "update.apk") }
                    Log.d(TAG, "onResponse:Download update.apk")

                    val file =
                        File(context.getExternalFilesDir("UPDATE").toString() + File.separator + "update.apk")
                    var fileUri = Uri.fromFile(file)

                    if (Build.VERSION.SDK_INT >= 24)
                        fileUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + ".provider",
                            file
                        )
                    val intent = Intent(Intent.ACTION_VIEW, fileUri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
                    intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                } else {
                    Log.d(TAG, "onResponse:Download update.apk ERROR!!")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.d(TAG, "onFailure:Download update.apk ERROR!!")
            }
        })
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, fileName: String): Boolean {
        return try {
            // todo change the file location/name according to your needs
            val file =
                File(context.getExternalFilesDir("UPDATE").toString() + File.separator + fileName)
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
}
