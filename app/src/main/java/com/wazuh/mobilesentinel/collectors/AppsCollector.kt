package com.wazuh.mobilesentinel.collectors

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object AppsCollector {

    fun collect(context: Context): Map<String, Any> {
        val packageManager = context.packageManager

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

        val userInstalledCount = packages.count { packageInfo ->
            val flags = packageInfo.applicationInfo?.flags ?: 0
            (flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }

        return mapOf(
            "installed_apps_count" to packages.size,
            "user_installed_count" to userInstalledCount
        )
    }
}
