package com.wazuh.mobilesentinel.collectors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkCollector {

    // We deliberately do NOT read the WiFi SSID here. On modern Android,
    // retrieving the SSID requires ACCESS_FINE_LOCATION, and this app is
    // intentionally minimizing the permissions it requests. connection_type
    // and vpn_active give useful security posture signal without it.
    fun collect(context: Context): Map<String, Any> {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val connectionType = when {
            capabilities == null -> "none"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }

        val vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)?.not() ?: false

        return mapOf(
            "connection_type" to connectionType,
            "vpn_active" to vpnActive,
            "is_metered" to isMetered
        )
    }
}
