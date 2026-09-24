package com.wazuh.mobilesentinel

object Constants {
    // Deployed EC2 Wazuh backend. Was http://10.0.2.2:5000/ingest (emulator's
    // alias for host localhost) during local Flask testing; switch back to that
    // for local dev, or to a LAN IP when testing on a real device.
    const val INGEST_URL = "http://54.252.57.123:5000/ingest"
}
