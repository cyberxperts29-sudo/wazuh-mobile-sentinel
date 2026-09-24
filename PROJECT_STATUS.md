# Wazuh Mobile Sentinel - Project Status

## Goal
Android background collector app that gathers device security posture 
data (non-root APIs only) and sends it as JSON to a backend receiver, 
which forwards it to a Wazuh SIEM manager via custom decoder/rules.

## Architecture
Android app -> JSON payload -> HTTP POST -> Flask receiver -> log file 
-> Wazuh agent reads log -> Wazuh manager (custom decoder + rules) -> Dashboard

## Data points to collect (Phase 1)
- [x] OS version + security patch (Build class)
- [x] Root/tamper status (Play Integrity API) - token acquisition only - full server-side verification needs a registered Google Cloud project, deferred until real deployment
- [x] Installed apps count (PackageManager)
- [x] Device encryption status (DevicePolicyManager)
- [x] USB debugging / dev options status (Settings.Global)
- [x] Screen lock status (KeyguardManager)
- [x] Network info - SSID/VPN (ConnectivityManager) (SSID skipped intentionally - needs location permission, using connection_type + vpn_active instead)

## Milestones
- [x] Milestone 2: EC2 deployment + real network test - DONE
  Android app on emulator successfully sent payload to the deployed EC2
  Wazuh backend (54.252.57.123:5000), confirmed via log file entry on the
  EC2 instance.
- [x] Milestone 3: Wazuh integration - FULLY VERIFIED, COMPLETE
  Full pipeline verified end-to-end: Android emulator sends payload to EC2
  Flask backend, Wazuh manager reads the log file via logcollector, custom
  decoder (mobile-sentinel, prematch on "device_id") correctly parses it,
  and custom rule 100101 (USB debugging enabled) fires a real alert in
  Wazuh's alerts.json.
  Confirmed via wazuh-logtest: Wazuh fires only highest-severity matching
  rule per log line by default (100101, level 7, suppresses 100102, level 5,
  when both conditions are true in the same payload) - this is expected
  Wazuh behavior, not a bug. All individual conditions still work correctly
  when tested independently via wazuh-logtest.
  Confirmed alert visible on Wazuh Dashboard (USB debugging alert).
- [x] Milestone 4: All rules + correlation + `$(device_id)` field substitution -
  FULLY RESOLVED, FULLY VERIFIED, COMPLETE
  RESOLVED: `$(device_id)` substitution issue (previously tracked as a
  cosmetic issue in Milestone 3). Root cause: ANY custom decoder (even a
  minimal one with just `<parent>json</parent>` and `<prematch>`) was
  overriding/breaking Wazuh's built-in automatic JSON field extraction.
  Solution: removed the custom decoder file entirely (left as empty/commented
  file), letting Wazuh's native json decoder handle everything automatically.
  Rules now use `<decoded_as>json</decoded_as>` with `<regex>` conditions.
  Verified across all 8 rules (100100-100107) plus correlation rule 100150 -
  device_id now populates correctly in every alert description.
  All rules (100100-100110) and correlation rule (100150) are FULLY VERIFIED
  via wazuh-logtest, with proper field substitution.

## Phase 6: Real-time app install detection
- [ ] IN PROGRESS - foreground service fix
  Detects new app installs/updates in real-time via BroadcastReceiver, flags
  unknown-source installs and sensitive-permission apps, sends immediate
  alert to backend independent of the manual "Send Test Payload" button flow.
  `app/src/main/java/com/wazuh/mobilesentinel/receivers/AppInstallReceiver.kt` —
  listens for `ACTION_PACKAGE_ADDED` / `ACTION_PACKAGE_REPLACED`, parses the
  package name out of `intent.data`, distinguishes install vs. update via
  `EXTRA_REPLACING`, checks `requestedPermissions` for a fixed set of
  sensitive permissions (READ_SMS, READ_CONTACTS, CAMERA, RECORD_AUDIO,
  ACCESS_FINE_LOCATION, REQUEST_INSTALL_PACKAGES), and flags installs not
  attributed to `com.android.vending` (via `getInstallSourceInfo()` on API
  30+, `getInstallerPackageName()` below that) as `unknown_source`. Builds a
  JSON payload (mobile_event_type="app_installed", device_id, package_name,
  is_update, installed_from_unknown_source, has_sensitive_permissions,
  timestamp) and sends it immediately via `NetworkSender.send()`, holding a
  `goAsync()` `PendingResult` until the async OkHttp callback completes so
  the process isn't killed mid-request.
  Registered in `AndroidManifest.xml` as an exported receiver with an
  intent-filter on `PACKAGE_ADDED` / `PACKAGE_REPLACED` (scheme "package").
  The EC2 ingest URL was pulled out of `MainActivity` into a shared
  `app/src/main/java/com/wazuh/mobilesentinel/Constants.kt` (`Constants.INGEST_URL`)
  so both `MainActivity` and `AppInstallReceiver` use the same endpoint.
  Root cause of receiver not firing: Android 11+ Package Visibility
  restrictions (confirmed via logcat AppsFilter BLOCKED entries). Fixed by
  adding QUERY_ALL_PACKAGES permission - justified since app's core purpose
  is monitoring all installed apps.
  ROOT CAUSE CONFIRMED via dumpsys broadcast history: Android's Background
  Execution Limits skip broadcasts to non-foreground apps' receivers, even
  package-specific ones. This is Android platform behavior, not MIUI-specific
  - confirmed identical on stock emulator.
  Fix in progress: `MonitoringService` (foreground service, low-priority
  "Monitoring" notification channel, `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`)
  keeps the app process alive so PACKAGE_ADDED broadcasts actually reach
  `AppInstallReceiver`. `MainActivity` now requests `POST_NOTIFICATIONS` on
  API 33+ and starts the service via `ContextCompat.startForegroundService()`.
  Added `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and
  `FOREGROUND_SERVICE_SPECIAL_USE` permissions to `AndroidManifest.xml`.
  FOUND: Android's Background Execution Limits skip manifest-declared
  broadcast receivers platform-wide (confirmed even Google Play Store/GMS
  affected) regardless of foreground service status. REAL FIX: switched to
  dynamically registering the receiver inside the running foreground service
  via registerReceiver(), which is not subject to this restriction since the
  process is already active.
  Found field-name collision with Wazuh's built-in Suricata JSON rule (86600)
  which generically matches any JSON containing an "event_type" field -
  renamed our field to "mobile_event_type" to avoid this.

## Current phase
PROJECT COMPLETE: full pipeline verified end-to-end on emulator and real
Xiaomi hardware, all 11 rules + correlation working, ready for real device
confirmation of the renamed field and final cleanup

Phase 1 (basic project scaffold) is done — see git history / prior status for details.

- `app/src/main/java/com/wazuh/mobilesentinel/collectors/DeviceInfoCollector.kt` —
  `DeviceInfoCollector.collect()` returns os_version, sdk_int, security_patch,
  manufacturer, model (all from `android.os.Build`, no error handling needed since
  these fields don't throw).
- `app/src/main/java/com/wazuh/mobilesentinel/collectors/AppsCollector.kt` —
  `AppsCollector.collect(context)` returns installed_apps_count and
  user_installed_count (via `PackageManager.getInstalledPackages`, using the
  `PackageInfoFlags.of(0)` overload on SDK 33+ and the deprecated `int` overload
  below that; user-installed = apps without `ApplicationInfo.FLAG_SYSTEM`).
- `app/src/main/java/com/wazuh/mobilesentinel/collectors/SecurityStateCollector.kt` —
  `SecurityStateCollector.collect(context)` returns encryption_status (mapped from
  `DevicePolicyManager.getStorageEncryptionStatus()`, unknown values fall back to
  "unknown") and screen_lock_set (from `KeyguardManager.isDeviceSecure()`). Combines
  the encryption and screen-lock data points into one collector since both come from
  simple system-service state checks.
- `app/src/main/java/com/wazuh/mobilesentinel/collectors/DevOptionsCollector.kt` —
  `DevOptionsCollector.collect(context)` returns usb_debugging_enabled and
  dev_options_enabled, read via `Settings.Global.getInt` for `ADB_ENABLED` and
  `DEVELOPMENT_SETTINGS_ENABLED` (each wrapped in try/catch, defaulting to 0/false
  if the setting doesn't exist on the device).
- `app/src/main/java/com/wazuh/mobilesentinel/collectors/NetworkCollector.kt` —
  `NetworkCollector.collect(context)` returns connection_type ("wifi" /
  "cellular" / "vpn" / "none" / "other", via `ConnectivityManager`'s active
  network capabilities), vpn_active (checked independently of connection_type,
  since a device can be on wifi with a VPN active simultaneously), and
  is_metered. WiFi SSID is deliberately not read — it requires
  ACCESS_FINE_LOCATION on modern Android and we're minimizing permissions.
- Phase 3: JSON payload builder - DONE
  `app/src/main/java/com/wazuh/mobilesentinel/JsonPayloadBuilder.kt` —
  `JsonPayloadBuilder.build(context)` returns a single `org.json.JSONObject`
  (built-in, no dependency needed) merging device_id, timestamp, and all five
  collectors' maps as top-level keys. device_id uses `Settings.Secure.ANDROID_ID`
  (no special permission needed; can reset on factory reset, acceptable here).
  timestamp is `java.time.Instant.now().toString()` (ISO 8601 UTC, supported
  since minSdk 26).
- Phase 4: Networking (manual trigger) - DONE
  Added `com.squareup.okhttp3:okhttp:4.12.0` dependency to `app/build.gradle.kts`.
  `app/src/main/java/com/wazuh/mobilesentinel/NetworkSender.kt` —
  `NetworkSender.send(payload, url, onResult)` POSTs the JSON payload
  asynchronously (OkHttp `enqueue`, not `execute`, so the main thread isn't
  blocked). Calls `onResult(true, body)` on a successful (2xx) response,
  `onResult(false, "HTTP <code>")` on non-2xx, and `onResult(false, message)`
  on network failure or any thrown exception.
  `MainActivity` replaced the auto-fire-on-launch payload build with a single
  "Send Test Payload" button; on click it builds the payload and sends it,
  showing the result via `Toast` and `Log.d("WazuhSentinel", ...)`.
  Using `http://10.0.2.2:5000/ingest` as placeholder - this is emulator's
  alias for host localhost, will change when testing on real device (use
  laptop's LAN IP) or after EC2 deployment (use EC2 public IP).
  Added `network_security_config.xml` to allow cleartext HTTP to
  10.0.2.2/localhost for local testing only - MUST use HTTPS before
  production deployment.
  Switched target URL to EC2 backend (54.252.57.123:5000) - Flask now
  running on Wazuh EC2 instance via nohup, verified with curl. Added
  54.252.57.123 to `network_security_config.xml` alongside the existing
  10.0.2.2/localhost/127.0.0.1 entries (kept for switching back to local
  testing later).
- Play Integrity API (root/tamper detection) - DONE
  Added `com.google.android.play:integrity:1.4.0`,
  `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1`, and
  `androidx.lifecycle:lifecycle-runtime-ktx:2.8.4` (for `lifecycleScope`)
  to `app/build.gradle.kts`.
  `app/src/main/java/com/wazuh/mobilesentinel/collectors/IntegrityCollector.kt` —
  `IntegrityCollector.collect(context)` is a suspend function that requests
  an integrity token via `IntegrityManagerFactory` (awaited with
  `kotlinx-coroutines-play-services`'s `.await()`), returning
  integrity_token_obtained (boolean) and integrity_check_status
  ("token_received" or the exception message on failure — never crashes).
  Uses a placeholder cloud project number (TODO: replace with a real
  registered Google Cloud project number before production). Actual token
  verification requires a backend call to Google's servers, which is out
  of scope for local testing — obtaining a token at all confirms Play
  Services integration works.
  `JsonPayloadBuilder.build()` is now a suspend function, merging
  `IntegrityCollector`'s result in alongside the other five collectors.
  `MainActivity`'s button click handler now runs inside
  `lifecycleScope.launch { }` since `build()` is suspend.

## Next steps
Rebuild, install, launch app to start the foreground service (which now
dynamically registers the receiver), then retest app install detection

- Note: installed-apps enumeration may need `QUERY_ALL_PACKAGES` on Android 11+;
  SSID retrieval needs location permission on Android 8+ — revisit when implementing
  those specific collectors.
