package com.wazuh.mobilesentinel.collectors

import android.os.Build

object DeviceInfoCollector {

    fun collect(): Map<String, Any> {
        return mapOf(
            "os_version" to Build.VERSION.RELEASE,
            "sdk_int" to Build.VERSION.SDK_INT,
            "security_patch" to Build.VERSION.SECURITY_PATCH,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL
        )
    }
}
