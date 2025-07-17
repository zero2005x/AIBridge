package com.aibridge.chat.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NetworkUtils"
        private const val GOOGLE_DNS = "8.8.8.8"
        private const val CLOUDFLARE_DNS = "1.1.1.1"
    }

    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Check if device can perform DNS resolution
     */
    suspend fun canResolveDNS(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Try to resolve Google DNS
            InetAddress.getByName(GOOGLE_DNS)
            Log.d(TAG, "DNS resolution working - can resolve $GOOGLE_DNS")
            true
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS resolution failed for $GOOGLE_DNS: ${e.message}")
            try {
                // Fallback to Cloudflare DNS
                InetAddress.getByName(CLOUDFLARE_DNS)
                Log.d(TAG, "DNS resolution working - can resolve $CLOUDFLARE_DNS")
                true
            } catch (e2: UnknownHostException) {
                Log.e(TAG, "DNS resolution completely failed: ${e2.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during DNS check: ${e.message}")
            false
        }
    }

    /**
     * Check if a specific hostname can be resolved
     */
    suspend fun canResolveHost(hostname: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanHostname = hostname.replace("https://", "").replace("http://", "").split("/")[0]
            InetAddress.getByName(cleanHostname)
            Log.d(TAG, "Successfully resolved hostname: $cleanHostname")
            true
        } catch (e: UnknownHostException) {
            Log.w(TAG, "Cannot resolve hostname $hostname: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving hostname $hostname: ${e.message}")
            false
        }
    }

    /**
     * Get detailed network connectivity information
     */
    fun getNetworkInfo(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        return NetworkInfo(
            isConnected = isNetworkAvailable(),
            hasWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
            hasCellular = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false,
            hasEthernet = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false,
            hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false,
            isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
        )
    }

    /**
     * Get user-friendly error message based on network state
     */
    suspend fun getNetworkErrorMessage(originalError: String): String {
        val networkInfo = getNetworkInfo()
        
        return when {
            !networkInfo.isConnected -> {
                "網路連線失敗：請檢查您的網路連線狀態"
            }
            !networkInfo.isValidated -> {
                "網路連線不穩定：請確認網路可以正常訪問互聯網"
            }
            !canResolveDNS() -> {
                "DNS解析失敗：請檢查網路設定或嘗試更換網路環境"
            }
            originalError.contains("Unable to resolve host") -> {
                "無法連接到伺服器：請檢查伺服器位址是否正確，或稍後重試"
            }
            originalError.contains("timeout") -> {
                "連線逾時：伺服器回應時間過長，請檢查網路環境或稍後重試"
            }
            originalError.contains("Connection refused") -> {
                "連線被拒絕：伺服器可能暫時無法使用"
            }
            else -> {
                "網路錯誤：$originalError"
            }
        }
    }
}

data class NetworkInfo(
    val isConnected: Boolean,
    val hasWifi: Boolean,
    val hasCellular: Boolean,
    val hasEthernet: Boolean,
    val hasInternet: Boolean,
    val isValidated: Boolean
) {
    override fun toString(): String {
        return "NetworkInfo(connected=$isConnected, wifi=$hasWifi, cellular=$hasCellular, " +
                "ethernet=$hasEthernet, internet=$hasInternet, validated=$isValidated)"
    }
}
