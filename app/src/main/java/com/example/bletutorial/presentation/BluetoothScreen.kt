package com.example.bletutorial.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.location.Location
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.location.LocationServices
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

    val context = LocalContext.current

    // Initialize FusedLocationProviderClient using the context
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState
    val scope = rememberCoroutineScope()
    var isCollecting by remember { mutableStateOf(false) }
    var isFound by remember { mutableStateOf(false) }

    val onConnectClick: (BluetoothDevice) -> Unit = { device ->
        // Ensure only one device connects at a time
        Log.d("BluetoothDeviceList", "Connecting to device: ${device.name}")
        viewModel.connectToDevice(device)
    }

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
                .fillMaxWidth(0.9f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            if(bleConnectionState == ConnectionState.CurrentlyInitializing){
                if (!isFound) {
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
                        if (viewModel.devices != null) {
                            isFound = true
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Found Devices:",
                            style = MaterialTheme.typography.h6
                        )

                        LazyColumn {
                            items(viewModel.devices ?: emptyList()) { device ->
                                DeviceItem(device = device, onConnectClick = onConnectClick)
                            }
                        }
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
                var recordId by remember { mutableStateOf("1") }
                var performerId by remember { mutableStateOf("100001") }
                var performanceLocation by remember { mutableStateOf("Shanghai") }
                var timestamp by remember { mutableStateOf("") }

                var gpsLatitude by remember { mutableStateOf(0.0) }
                var gpsLongitude by remember { mutableStateOf(0.0) }
                var gpsAltitude by remember { mutableStateOf(0.0) }

                val dataList = mutableListOf<DataInfo>()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${viewModel.deviceName} device connected successfully.",
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
                                        val durationMillis = 1000L // 4 seconds
                                        val pollingIntervalMillis = 10L // Poll every 100 ms
                                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")  // Updated format pattern
                                        val initStartTime = System.currentTimeMillis()
                                        val initDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(initStartTime), ZoneId.systemDefault())
                                        val initTimestamp = initDateTime.format(formatter)

                                        val performanceData = PerformanceData(performerId.toInt(), initTimestamp, performanceLocation)

                                        while (isCollecting) {
                                            var startTime = System.currentTimeMillis()
                                            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                                            val smallPackage = mutableListOf<String>()
                                            val largePackage = mutableListOf<String>()
                                            val seconds = 4
                                            timestamp = dateTime.format(formatter)

                                            for (i in 1..seconds) {
                                                startTime = System.currentTimeMillis()
                                                val currentSmall = mutableListOf<String>()
                                                val currentLarge = mutableListOf<String>()
                                                while (System.currentTimeMillis() - startTime < durationMillis && (currentSmall.size < 512 || currentLarge.size < 1)) {
                                                    // Append bluetooth data from viewModel to the list

                                                    val hexString = viewModel.bluetoothData.toHex()

                                                    val (small, large) = filterPackage(hexString)

                                                    if (currentSmall.size < 512) {
                                                        val remainingSpace = 512 - currentSmall.size
                                                        currentSmall.addAll(small.take(remainingSpace))
                                                    }

                                                    if (currentLarge.size < 1) {
                                                        currentLarge.addAll(large.take(1 - currentLarge.size))
                                                    }
                                                    delay(pollingIntervalMillis)
                                                }
                                                smallPackage.addAll(currentSmall)
                                                largePackage.addAll(currentLarge)
                                                Log.d("packageSize", "small size ${currentSmall.size}")
                                                Log.d("packageSize", "large size ${currentLarge.size}")
                                            }

                                            // Locate the location
                                            fusedLocationClient.lastLocation
                                                .addOnSuccessListener { location: Location? ->
                                                    if (location != null) {
                                                        gpsLatitude = location.latitude
                                                        gpsLongitude = location.longitude
                                                        gpsAltitude = location.altitude

                                                    }
                                                }

                                            val blobData = BlobData(smallPackage, largePackage)
                                            val performanceRecords = mutableListOf<PerformanceRecords>(PerformanceRecords(recordId.toInt(), timestamp, gpsLatitude, gpsLongitude, gpsAltitude, blobData))


                                            val dataInfo = DataInfo(performanceData, performanceRecords)
                                            dataList.add(dataInfo)
                                            Log.d("API", "PostData: $dataInfo")

                                            sendAPI(dataInfo)

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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Takes up available space
                            .heightIn(max = 200.dp)
                    ) {
                        items(dataList) { dataInfo ->
                            DataInfoItem(dataInfo = dataInfo, onResendClick = { sendAPI(dataInfo) })
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

fun filterPackage(hexString: String): Pair<List<String>, List<String>> {
    val smallRegex = Regex("(aaaa048002[0-9a-f]+?)(?=aaaa)(?!$)")
    val largeRegex = Regex("(aaaa20[0-9a-f]+?)(?=aaaa)(?!$)")

    val smallMatches = smallRegex.findAll(hexString).map { it.value }.toList()
    val largeMatches = largeRegex.findAll(hexString).map { it.value }.toList()

    return Pair(smallMatches, largeMatches)
}

fun sendAPI (dataInfo: DataInfo) {
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
}

@Composable
fun DeviceItem(device: BluetoothDevice, onConnectClick: (BluetoothDevice) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Optionally handle item click if needed
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = device.name ?: "Unnamed Device")
            Text(text = device.address)
        }

        Button(onClick = { onConnectClick(device) }) {
            Text(text = "Connect")
        }
    }
}

@Composable
fun DataInfoItem(dataInfo: DataInfo, onResendClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Text(text = "Timestamp: ${dataInfo.performanceData?.performanceTime}")
        Spacer(modifier = Modifier.height(8.dp))

        // Button to resend the API for this specific item
        Button(onClick = onResendClick) {
            Text("Resend")
        }
    }
}


