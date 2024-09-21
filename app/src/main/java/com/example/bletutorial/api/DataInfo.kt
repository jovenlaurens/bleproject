package com.example.bletutorial.api

import com.google.gson.annotations.SerializedName

data class DataInfo(
    @SerializedName("performance_data") val performanceData: PerformanceData?,
    @SerializedName("performance_records") val performanceRecords: PerformanceRecords?
)

data class PerformanceData (
    @SerializedName("performer_id") val performerId: Int?,
    @SerializedName("performance_time") val performanceTime: String?,
    @SerializedName("performance_location") val performanceLocation: String?
)

data class PerformanceRecords (
    @SerializedName("record_id") val recordID: Int?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("gps_latitude") val gpsLatitude: Float?,
    @SerializedName("gps_longitude") val gpsLongitude: Float?,
    @SerializedName("gps_altitude") val gpsAltitude: Float?,
    @SerializedName("blob_data") val blobData: List<String>?
)
