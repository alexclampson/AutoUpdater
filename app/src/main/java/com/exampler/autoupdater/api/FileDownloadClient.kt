package com.exampler.autoupdater.api


import com.exampler.autoupdater.LATEST_VERSION_APK
import com.exampler.autoupdater.LATEST_VERSION_URL
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface FileDownloadClient {

    @GET(LATEST_VERSION_URL)
    fun downloadLastVersionInfo(): Call<ResponseBody?>?

    @GET(LATEST_VERSION_APK)
    fun downloadLastApk(): Call<ResponseBody?>

}
