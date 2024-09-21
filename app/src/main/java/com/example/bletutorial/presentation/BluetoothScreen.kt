package com.example.bletutorial.presentation

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.bletutorial.api.BlobData
import com.example.bletutorial.api.DataInfo
import com.example.bletutorial.api.PerformanceData
import com.example.bletutorial.api.PerformanceRecords
import com.example.bletutorial.api.service
import com.example.bletutorial.data.ConnectionState
import com.example.bletutorial.presentation.permissions.PermissionUtils
import com.example.bletutorial.presentation.permissions.SystemBroadcastReceiver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothScreen(
    onBluetoothStateChanged:()->Unit,
    viewModel: DataViewModel = hiltViewModel()
) {
    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED){ bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if(action == BluetoothAdapter.ACTION_STATE_CHANGED){
            onBluetoothStateChanged()
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState
    val scope = rememberCoroutineScope()
    var accumulatedData by remember { mutableStateOf("") }
    var accumulatedRawData by remember { mutableStateOf("") }
    var isCollecting by remember { mutableStateOf(false) } // To track if data collection is active
    var stopRequested by remember { mutableStateOf(false) }

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver{_,event ->
                if(event == Lifecycle.Event.ON_START){
                    permissionState.launchMultiplePermissionRequest()
                    if(permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected){
                        viewModel.reconnect()
                    }
                }
                if(event == Lifecycle.Event.ON_STOP){
                    if (bleConnectionState == ConnectionState.Connected){
                        viewModel.disconnect()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    LaunchedEffect(key1 = permissionState.allPermissionsGranted){
        if(permissionState.allPermissionsGranted){
            if(bleConnectionState == ConnectionState.Uninitialized){
                viewModel.initializeConnection()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            if(bleConnectionState == ConnectionState.CurrentlyInitializing){
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator()
                    if(viewModel.initializingMessage != null){
                        Text(
                            text = viewModel.initializingMessage!!
                        )
                    }
                }
            }else if(!permissionState.allPermissionsGranted){
                Text(
                    text = "Go to the app setting and allow the missing permissions.",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }else if(viewModel.errorMessage != null){
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage!!
                    )
                    Button (
                        onClick = {
                            if(permissionState.allPermissionsGranted){
                                viewModel.initializeConnection()
                            }
                        }
                    ) {
                        Text(
                            "Try again"
                        )
                    }
                }
            }else if(bleConnectionState == ConnectionState.Connected){
                var recordId by remember { mutableStateOf("0") }
                var performerId by remember { mutableStateOf("0") }
                var performanceLocation by remember { mutableStateOf("Shanghai") }
                var timestamp by remember { mutableStateOf("") }

                var gpsLatitude by remember { mutableStateOf(40.7128f) }
                var gpsLongitude by remember { mutableStateOf(40.7128f) }
                var gpsAltitude by remember { mutableStateOf(40.7128f) }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Bluetooth device connected successfully.",
                        style = MaterialTheme.typography.h6
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = recordId,
                        onValueChange = { recordId = it },
                        label = { Text("Record ID") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = performerId,
                        onValueChange = { performerId = it },
                        label = { Text("Performer ID") },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = performanceLocation,
                        onValueChange = { performanceLocation = it },
                        label = { Text("Performance Location") },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCollecting) {
                        Text(
                            text = "Timestamp: $timestamp",
                            style = MaterialTheme.typography.h6
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Recording...",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.fillMaxWidth(0.5f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to stop data collection
                        Button(
                            onClick = {
                                isCollecting = false
                                Log.d("test", "Recording data stopped")
                            },
                            modifier = Modifier.fillMaxWidth(0.5f),
                            enabled = isCollecting // Enable the button only when collecting
                        ) {
                            Text("Stop")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!isCollecting) {
                                    isCollecting = true
                                    Log.d("test", "Recording data started")

                                    // Launch a coroutine for data collection
                                    scope.launch {
                                        val durationMillis = 4000L // 4 seconds
                                        val pollingIntervalMillis = 100L // Poll every 100 ms
                                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")  // Updated format pattern
                                        val initStartTime = System.currentTimeMillis()
                                        val initDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(initStartTime), ZoneId.systemDefault())
                                        val initTimestamp = initDateTime.format(formatter)

                                        val performanceData = PerformanceData(performerId.toInt(), initTimestamp, performanceLocation)

                                        while (isCollecting) {
                                            val rawDataList = mutableListOf<String>()
                                            val bluetoothDataList = mutableListOf<String>()
                                            val startTime = System.currentTimeMillis()

                                            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                                            timestamp = dateTime.format(formatter)

                                            while (System.currentTimeMillis() - startTime < durationMillis) {
                                                // Append bluetooth data from viewModel to the list
                                                bluetoothDataList.add(viewModel.bluetoothData.toHex())
                                                rawDataList.add(viewModel.bluetoothData.toString())

                                                val byteSize = countBytesFromHex(viewModel.bluetoothData.toHex())
                                                Log.d("dataSize", "byte size is $byteSize bytes")

                                                // Wait for the polling interval before appending again
                                                delay(pollingIntervalMillis)
                                            }

                                            // Concatenate the collected data
                                            accumulatedData = bluetoothDataList.joinToString("\n")
                                            accumulatedRawData = rawDataList.joinToString("\n")

                                            val blobData = BlobData(bluetoothDataList, bluetoothDataList)
                                            val performanceRecords = mutableListOf<PerformanceRecords>(PerformanceRecords(recordId.toInt(), timestamp, gpsLatitude, gpsLongitude, gpsAltitude, blobData))

                                            Log.d("dataCollected", accumulatedData)
                                            Log.d("rawDataCollected", accumulatedRawData)

                                            val dataInfo = DataInfo(performanceData, performanceRecords)
                                            Log.d("API", "PostData: $dataInfo")

                                            service.sendData(dataInfo).enqueue(object : retrofit2.Callback<Void> {
                                                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                                                    if (response.isSuccessful) {
                                                        Log.d("API", "Post successful!")
                                                    } else {
                                                        Log.d("API", "Error: ${response.code()}")
                                                    }
                                                }

                                                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                                                    Log.d("API", "Request failed: ${t.message}")
                                                }
                                            })

                                            accumulatedData = ""
                                            accumulatedRawData = ""
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.5f),
                        ) {
                            Text("Record Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

            }else if(bleConnectionState == ConnectionState.Disconnected){
                Button(onClick = {
                    viewModel.initializeConnection()
                }) {
                    Text("Initialize again")
                }
            }
        }
    }
}

fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte -> "%02x".format(byte) }
}

fun countBytesFromHex(hexString: String): Int {
    // Remove any whitespace or unwanted characters
    val cleanedHex = hexString.replace("\\s".toRegex(), "") // Remove any spaces or non-hex chars

    // Each pair of hex digits (2 characters) represents 1 byte
    return cleanedHex.length / 2
}