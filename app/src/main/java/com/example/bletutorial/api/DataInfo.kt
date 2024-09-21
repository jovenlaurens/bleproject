package com.example.bletutorial.api

import com.google.gson.annotations.SerializedName

data class DataInfo(
    @SerializedName("performance_data") val performanceData: PerformanceData?,
    @SerializedName("performance_records") val performanceRecords: PerformanceRecords?
)

data class PerformanceData (
    @SerializedName("performer_id") val performerId: Int?,
    @SerializedName("performance_time") val performanceTime: String?,
)

data class PerformanceRecords (
    @SerializedName("record_id") val recordID: Int?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("blob_data") val blobData: List<String>?
)
