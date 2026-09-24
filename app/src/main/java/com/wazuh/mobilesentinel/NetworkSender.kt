package com.wazuh.mobilesentinel

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NetworkSender {

    private val client = OkHttpClient()
    private val JSON = "application/json".toMediaType()

    fun send(payload: JSONObject, url: String, onResult: (Boolean, String) -> Unit) {
        try {
            val body = payload.toString().toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onResult(false, e.message ?: "Network request failed")
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    response.use {
                        if (it.isSuccessful) {
                            onResult(true, it.body?.string() ?: "OK")
                        } else {
                            onResult(false, "HTTP ${it.code}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            onResult(false, e.message ?: "Unknown error")
        }
    }
}
