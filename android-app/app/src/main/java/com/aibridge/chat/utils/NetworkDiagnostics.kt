package com.aibridge.chat.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDiagnostics @Inject constructor(
    private val networkUtils: NetworkUtils
) {
    companion object {
        private const val TAG = "NetworkDiagnostics"
    }
    
    suspend fun runFullDiagnostics(targetUrl: String): NetworkDiagnosticResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Running full network diagnostics for: $targetUrl")
        
        val results = mutableMapOf<String, Boolean>()
        val errors = mutableListOf<String>()
        
        try {
            // 1. Check basic network connectivity
            val isNetworkAvailable = networkUtils.isNetworkAvailable()
            results["network_available"] = isNetworkAvailable
            if (!isNetworkAvailable) {
                errors.add("No network connectivity")
            }
            
            // 2. Check DNS resolution for common sites
            val canResolveDNS = networkUtils.canResolveDNS()
            results["dns_working"] = canResolveDNS
            if (!canResolveDNS) {
                errors.add("DNS resolution not working")
            }
            
            // 3. Check if we can resolve the target hostname
            val canResolveTarget = networkUtils.canResolveHost(targetUrl)
            results["target_resolvable"] = canResolveTarget
            if (!canResolveTarget) {
                errors.add("Cannot resolve target hostname: $targetUrl")
            }
            
            // 4. Try to get IP address of target
            var targetIpAddress: String? = null
            try {
                val cleanHostname = targetUrl.replace("https://", "").replace("http://", "").split("/")[0]
                val inetAddress = InetAddress.getByName(cleanHostname)
                targetIpAddress = inetAddress.hostAddress
                results["target_ip_resolved"] = true
                Log.d(TAG, "Target IP address: $targetIpAddress")
            } catch (e: Exception) {
                results["target_ip_resolved"] = false
                errors.add("Cannot get IP address for target: ${e.message}")
            }
            
            // 5. Try basic HTTP connectivity test
            if (canResolveTarget) {
                val canConnect = testHttpConnectivity(targetUrl)
                results["http_connectivity"] = canConnect
                if (!canConnect) {
                    errors.add("HTTP connectivity test failed")
                }
            }
            
            // 6. Network info
            val networkInfo = networkUtils.getNetworkInfo()
            results["has_wifi"] = networkInfo.hasWifi
            results["has_cellular"] = networkInfo.hasCellular
            results["has_ethernet"] = networkInfo.hasEthernet
            results["is_validated"] = networkInfo.isValidated
            
            NetworkDiagnosticResult(
                success = results["network_available"] == true && results["dns_working"] == true,
                results = results,
                errors = errors,
                targetIpAddress = targetIpAddress,
                networkInfo = networkInfo.toString()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Diagnostic error: ${e.message}", e)
            errors.add("Diagnostic failed: ${e.message}")
            
            NetworkDiagnosticResult(
                success = false,
                results = results,
                errors = errors,
                targetIpAddress = null,
                networkInfo = "Error getting network info"
            )
        }
    }
    
    private suspend fun testHttpConnectivity(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
                
            val request = Request.Builder()
                .url(url)
                .head() // Use HEAD request to avoid downloading content
                .build()
                
            val response = client.newCall(request).execute()
            val success = response.isSuccessful || response.code in 300..399 // Allow redirects
            response.close()
            
            Log.d(TAG, "HTTP connectivity test result: $success (code: ${response.code})")
            success
        } catch (e: Exception) {
            Log.w(TAG, "HTTP connectivity test failed: ${e.message}")
            false
        }
    }
}

data class NetworkDiagnosticResult(
    val success: Boolean,
    val results: Map<String, Boolean>,
    val errors: List<String>,
    val targetIpAddress: String?,
    val networkInfo: String
) {
    fun getSummary(): String {
        val builder = StringBuilder()
        builder.append("Network Diagnostic Summary:\n")
        builder.append("Overall Success: $success\n")
        builder.append("Target IP: ${targetIpAddress ?: "N/A"}\n")
        builder.append("Network Info: $networkInfo\n")
        builder.append("Results:\n")
        results.forEach { (key, value) ->
            builder.append("  $key: $value\n")
        }
        if (errors.isNotEmpty()) {
            builder.append("Errors:\n")
            errors.forEach { error ->
                builder.append("  - $error\n")
            }
        }
        return builder.toString()
    }
}
