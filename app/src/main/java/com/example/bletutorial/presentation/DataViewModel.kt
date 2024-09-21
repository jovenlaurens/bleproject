package com.example.bletutorial.presentation

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bletutorial.data.ConnectionState
import com.example.bletutorial.data.DataReceiveManager
import com.example.bletutorial.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor( private val dataReceiveManager: DataReceiveManager) : ViewModel() {
    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var bluetoothData by mutableStateOf(ByteArray(0))
        private set

    var devices by mutableStateOf<List<BluetoothDevice>?>(emptyList())
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private fun subscribeToChanges(){
        viewModelScope.launch {
            dataReceiveManager.data.collect{ result ->
                when(result){
                    is Resource.Success -> {
                        connectionState = result.data.connectionState
                        bluetoothData = result.data.bluetoothData
                    }

                    is Resource.Loading -> {
                        devices = result.data?.bluetoothDevices
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitializing
                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        dataReceiveManager.connect(device)
    }

    fun disconnect(){
        dataReceiveManager.disconnect()
    }

    fun reconnect(){
        dataReceiveManager.reconnect()
    }

    fun initializeConnection(){
        errorMessage = null
        subscribeToChanges()
        dataReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        dataReceiveManager.closeConnection()
    }
}