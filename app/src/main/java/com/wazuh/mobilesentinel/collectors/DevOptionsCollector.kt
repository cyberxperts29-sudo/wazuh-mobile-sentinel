package com.wazuh.mobilesentinel.collectors

import android.content.Context
import android.provider.Settings

object DevOptionsCollector {

    fun collect(context: Context): Map<String, Any> {
        val resolver = context.contentResolver

        val adbEnabled = try {
            Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        val devOptionsEnabled = try {
            Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        return mapOf(
            "usb_debugging_enabled" to (adbEnabled == 1),
            "dev_options_enabled" to (devOptionsEnabled == 1)
        )
    }
}
