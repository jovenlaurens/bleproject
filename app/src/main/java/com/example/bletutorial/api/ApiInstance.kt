package com.example.bletutorial.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("http://127.0.0.1:5000")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val service = retrofit.create(RestApi::class.java)