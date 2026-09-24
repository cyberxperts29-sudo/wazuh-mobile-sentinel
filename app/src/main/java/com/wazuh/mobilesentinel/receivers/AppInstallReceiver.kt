// Manifest-declared receivers for PACKAGE_ADDED about OTHER apps are exempt
// from Android 8+ implicit broadcast restrictions since they're
// package-specific broadcasts, not general implicit ones - this should work
// without needing a foreground service. Test thoroughly on real device to
// confirm delivery timing.
package com.wazuh.mobilesentinel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.wazuh.mobilesentinel.Constants
import com.wazuh.mobilesentinel.NetworkSender
import org.json.JSONObject
import java.time.Instant

private val SENSITIVE_PERMISSIONS = setOf(
    "android.permission.READ_SMS",
    "android.permission.READ_CONTACTS",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.REQUEST_INSTALL_PACKAGES"
)

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED &&
            intent.action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }

        val packageName = intent.data?.schemeSpecificPart ?: return
        val isUpdate = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        val packageManager = context.packageManager

        val hasSensitivePermissions = hasSensitivePermissions(packageManager, packageName)
        val installedFromUnknownSource = isFromUnknownSource(packageManager, packageName)

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val payload = JSONObject()
        payload.put("mobile_event_type", "app_installed")
        payload.put("device_id", deviceId)
        payload.put("package_name", packageName)
        payload.put("is_update", isUpdate)
        payload.put("installed_from_unknown_source", installedFromUnknownSource)
        payload.put("has_sensitive_permissions", hasSensitivePermissions)
        payload.put("timestamp", Instant.now().toString())

        // Hold a PendingResult so the process isn't killed before the async
        // OkHttp callback (NetworkSender.send uses enqueue, not execute) completes.
        val pendingResult = goAsync()
        NetworkSender.send(payload, Constants.INGEST_URL) { success, message ->
            Log.d("WazuhSentinel", "app_installed send success=$success message=$message")
            pendingResult.finish()
        }
    }

    private fun hasSensitivePermissions(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
            requestedPermissions.any { it in SENSITIVE_PERMISSIONS }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isFromUnknownSource(packageManager: PackageManager, packageName: String): Boolean {
        val installerPackageName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        return installerPackageName != "com.android.vending"
    }
}
