package com.example.bletutorial.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RestApi {
    @POST("endpoint")
    fun sendData(@Body postData: DataInfo): Call<Void>
}