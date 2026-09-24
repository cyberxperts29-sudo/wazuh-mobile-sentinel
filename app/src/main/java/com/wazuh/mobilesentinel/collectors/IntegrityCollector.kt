package com.wazuh.mobilesentinel.collectors

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

// TODO: Replace with real Google Cloud project number before production use - register at Play Console
private const val CLOUD_PROJECT_NUMBER = 123456789012L

object IntegrityCollector {

    suspend fun collect(context: Context): Map<String, Any> {
        return try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .setNonce(generateNonce())
                .build()

            integrityManager.requestIntegrityToken(request).await()

            // Actual token verification requires sending this token to a backend
            // that calls Google's servers with the registered cloud project -
            // that's out of scope for local testing. Successfully obtaining a
            // token at all confirms Play Services integration works.
            mapOf(
                "integrity_token_obtained" to true,
                "integrity_check_status" to "token_received"
            )
        } catch (e: Exception) {
            mapOf(
                "integrity_token_obtained" to false,
                "integrity_check_status" to (e.message ?: "unknown error")
            )
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
