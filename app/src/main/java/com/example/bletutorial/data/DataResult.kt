package com.example.bletutorial.data

data class DataResult (
    val bluetoothData: ByteArray,
    val connectionState: ConnectionState
)