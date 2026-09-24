package com.wazuh.mobilesentinel

import android.content.Context
import android.provider.Settings
import com.wazuh.mobilesentinel.collectors.AppsCollector
import com.wazuh.mobilesentinel.collectors.DevOptionsCollector
import com.wazuh.mobilesentinel.collectors.DeviceInfoCollector
import com.wazuh.mobilesentinel.collectors.IntegrityCollector
import com.wazuh.mobilesentinel.collectors.NetworkCollector
import com.wazuh.mobilesentinel.collectors.SecurityStateCollector
import org.json.JSONObject
import java.time.Instant

object JsonPayloadBuilder {

    suspend fun build(context: Context): JSONObject {
        // ANDROID_ID is app-and-device-specific and needs no special permission.
        // It can reset on factory reset (and differs per app signing key), but
        // that's acceptable for this use case — we just need a stable-enough
        // identifier between resets, not a permanent hardware ID.
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val payload = JSONObject()
        payload.put("device_id", deviceId)
        payload.put("timestamp", Instant.now().toString())

        val collected = listOf(
            DeviceInfoCollector.collect(),
            AppsCollector.collect(context),
            SecurityStateCollector.collect(context),
            DevOptionsCollector.collect(context),
            NetworkCollector.collect(context),
            IntegrityCollector.collect(context)
        )

        for (map in collected) {
            for ((key, value) in map) {
                payload.put(key, value)
            }
        }

        return payload
    }
}
