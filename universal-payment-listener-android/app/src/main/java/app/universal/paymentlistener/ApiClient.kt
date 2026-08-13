package app.universal.paymentlistener

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    data class Response(val code: Int, val body: String)

    fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()): Response {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = if (stream != null) BufferedReader(InputStreamReader(stream)).use { it.readText() } else ""
        return Response(conn.responseCode, text)
    }

    fun jsonObject(response: Response): JSONObject? = try {
        JSONObject(response.body)
    } catch (_: Exception) {
        null
    }
}
