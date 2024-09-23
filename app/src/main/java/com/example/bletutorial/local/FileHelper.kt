package com.example.bletutorial.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.bletutorial.api.DataInfo
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.IOException

private fun writeDataToFile(context: Context, uri: Uri, dataInfo: DataInfo) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val json = Gson().toJson(dataInfo) // Your dataInfo object serialized to JSON
            outputStream.write(json.toByteArray())
        }
        Log.d("BluetoothScreen", "File saved successfully")
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("BluetoothScreen", "Failed to save file: ${e.message}")
    }
}

class FileHelper(private val context: Context) {
    private val gson = Gson()

    // Function to save DataInfo as a JSON file
    fun saveDataInfoAsJson(fileName: String, dataInfo: DataInfo) {
        val gson = Gson()
        val jsonString = gson.toJson(dataInfo)

        // Get the Downloads directory
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (!downloadsDirectory.exists()) {
            downloadsDirectory.mkdirs() // Create the directory if it doesn't exist
        }

        val file = File(downloadsDirectory, fileName)
        try {
            val fileWriter = FileWriter(file)
            fileWriter.write(jsonString)
            fileWriter.close()
            Log.d("FileHelper", "File saved successfully at: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("FileHelper", "Error saving file: ${e.message}")
        }
    }

    // Function to read DataInfo from a JSON file
    fun readDataInfoFromJson(fileName: String): DataInfo? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            val jsonString = file.readText()
            gson.fromJson(jsonString, DataInfo::class.java)
        } else {
            null // Return null if the file doesn't exist
        }
    }
}
