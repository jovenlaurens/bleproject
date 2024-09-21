package com.example.bletutorial.data

import android.bluetooth.BluetoothDevice

data class DataResult (
    val bluetoothDevices: List<BluetoothDevice>?,
    val device: String,
    val bluetoothData: ByteArray,
    val connectionState: ConnectionState
)