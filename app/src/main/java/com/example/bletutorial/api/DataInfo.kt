package com.example.bletutorial.api

import com.google.gson.annotations.SerializedName

data class DataInfo(
    @SerializedName("performance_data") val performanceData: PerformanceData?,
    @SerializedName("performance_records") val performanceRecords: List<PerformanceRecords>?
)

data class PerformanceData (
    @SerializedName("performer_id") val performerId: Int?,
    @SerializedName("performance_time") val performanceTime: String?,
    @SerializedName("performance_location") val performanceLocation: String?
)

data class PerformanceRecords (
    @SerializedName("record_id") val recordID: Int?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("gps_latitude") val gpsLatitude: Double?,
    @SerializedName("gps_longitude") val gpsLongitude: Double?,
    @SerializedName("gps_altitude") val gpsAltitude: Double?,
    @SerializedName("blob_data") val blobData: BlobData?
)

data class BlobData (
    @SerializedName("rawdata") val rawData: List<String>?,
    @SerializedName("finalpackage") val finalPackage: List<String>?
    // @SerializedName("label") val label: Int?
)


