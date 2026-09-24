package com.wazuh.mobilesentinel.collectors

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context

object SecurityStateCollector {

    fun collect(context: Context): Map<String, Any> {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        val encryptionStatus = when (devicePolicyManager.storageEncryptionStatus) {
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "unsupported"
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "inactive"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "activating"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "active"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "active_default_key"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "active_per_user"
            else -> "unknown"
        }

        return mapOf(
            "encryption_status" to encryptionStatus,
            "screen_lock_set" to keyguardManager.isDeviceSecure
        )
    }
}
