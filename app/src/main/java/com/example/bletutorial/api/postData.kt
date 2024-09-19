
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun postBluetoothData(data: ByteArray) {
    val url = URL("http://127.0.0.1:5000/add_performance_and_records")

    // Construct JSON data
    val jsonData = JSONObject().apply {
        put("performance_data", JSONObject().apply {
            put("performer_id", 101)
            put("performance_time", "2024-09-16 14:00:00")
            put("performance_location", "New York")
        })
        put("performance_records", JSONArray().apply {
            put(JSONObject().apply {
                put("record_id", 1)
                put("timestamp", "2024-09-16 14:10:00")
                put("gps_latitude", 40.7128)
                put("gps_longitude", -74.0060)
                put("gps_altitude", 15.0)
                put("blob_data", data.joinToString(", ") { byte -> byte.toInt().toString() })
            })
        })
    }

    // Perform the network request in IO dispatcher
    withContext(Dispatchers.IO) {
        (url.openConnection() as? HttpURLConnection)?.run {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")

            // Write JSON data to the request body
            outputStream.write(jsonData.toString().toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Handle the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                println("Data successfully sent.")
            } else {
                println("Error: $responseCode")
            }
        }
    }
}
