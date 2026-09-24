package com.wazuh.mobilesentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Single, minimal activity. This app is a background data collector, not a
 * consumer-facing UI — the activity exists only so the app has a launch
 * point; the real work happens in collectors/services added in later phases.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            lifecycleScope.launch {
                val payload = JsonPayloadBuilder.build(this@MainActivity)
                NetworkSender.send(payload, Constants.INGEST_URL) { success, message ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        Log.d("WazuhSentinel", "success=$success message=$message")
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        // Keeps the app's process alive so Android delivers PACKAGE_ADDED
        // broadcasts to AppInstallReceiver - see MonitoringService for why.
        ContextCompat.startForegroundService(this, Intent(this, MonitoringService::class.java))
    }
}
