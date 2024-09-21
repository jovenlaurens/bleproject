package com.example.bletutorial.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.bletutorial.data.ConnectionState
import com.example.bletutorial.data.DataReceiveManager
import com.example.bletutorial.data.DataResult
import com.example.bletutorial.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class DataBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : DataReceiveManager {

    private val DATA_SERVICE_UIID = "0000ffe0-0000-1000-8000-00805f9b34fb"
    private val DATA_CHARACTERISTICS_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

    private val bleDevices = mutableListOf<BluetoothDevice>()

    private var deviceName = ""

    override val data: MutableSharedFlow<Resource<DataResult>> = MutableSharedFlow()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val scanCallback = object : ScanCallback(){

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device

            if (!bleDevices.contains(device) && !device.name.isNullOrEmpty()) {
                bleDevices.add(device)
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Discovering devices"))
                }
            } else {
                coroutineScope.launch {
                    data.emit(Resource.Loading(data = DataResult(
                        bleDevices,
                        deviceName,
                        ByteArray(0),
                        ConnectionState.Disconnected
                    ), message = "Discovered devices!"))
                }
            }

            Log.d("Bluetooth Device", bleDevices.toString())
        }

    }

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(data = DataResult(
                            bleDevices,
                            deviceName,
                            ByteArray(0),
                            ConnectionState.Disconnected
                        ),message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@DataBLEReceiveManager.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = DataResult(
                                    bleDevices,
                                    deviceName,
                                    ByteArray(0),
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    gatt.close()
                }
            } else {
                gatt.close()
                currentConnectionAttempt += 1
                coroutineScope.launch {
                    data.emit(Resource.Loading(data = DataResult(
                        bleDevices,
                        deviceName,
                        ByteArray(0),
                        ConnectionState.Disconnected
                    ),message = "Attempting to connect $currentConnectionAttempt / $MAXIMUM_CONNECTION_ATTEMPTS"))
                }

                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    startReceiving()
                } else {
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to ble device"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt){
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(data = DataResult(
                        bleDevices,
                        deviceName,
                        ByteArray(0),
                        ConnectionState.Disconnected
                    ),message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic = findCharacteristics(DATA_SERVICE_UIID, DATA_CHARACTERISTICS_UUID)
            if(characteristic == null){
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find data publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            coroutineScope.launch {
                try {
                    when (characteristic.uuid) {
                        UUID.fromString(DATA_CHARACTERISTICS_UUID) -> {
                            val rawData = characteristic.value

                            // Simulating data processing
                            val dataResult = DataResult(
                                bleDevices,
                                deviceName,
                                rawData,
                                ConnectionState.Connected
                            )
                            data.emit(Resource.Success(data = dataResult))
                        }
                        else -> Unit
                    }
                } catch (e: Exception) {
                    data.emit(Resource.Error(errorMessage = "Error processing characteristic change"))
                }
            }
            }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray){
        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID:String):BluetoothGattCharacteristic?{
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun connect(device: BluetoothDevice) {
        Log.d("Connecting", "device name: ${device.name}")
        deviceName = device.name
        coroutineScope.launch {
            data.emit(Resource.Loading(data = DataResult(
                bleDevices,
                deviceName,
                ByteArray(0),
                ConnectionState.Connected
            ),message = "Connecting to device..."))
        }
        if(isScanning){
            device.connectGatt(context,false, gattCallback)
            isScanning = false
            bleScanner.stopScan(scanCallback)
        }
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(data = DataResult(
                bleDevices,
                deviceName,
                ByteArray(0),
                ConnectionState.Disconnected
            ),message = "Scanning Ble devices..."))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristics(DATA_SERVICE_UIID, DATA_CHARACTERISTICS_UUID)
        if(characteristic != null){
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic,false) == false){
                Log.d("DataReceiveManager","set charateristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }
}