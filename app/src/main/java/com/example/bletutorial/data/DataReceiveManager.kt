package com.example.bletutorial.data

import com.example.bletutorial.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface DataReceiveManager {
    val data: MutableSharedFlow<Resource<DataResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()
}