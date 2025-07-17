package com.aibridge.chat.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生物識別認證幫助器
 */
@Singleton
class BiometricAuthHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * 檢查設備是否支援生物識別認證
     */
    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.TEMPORARILY_NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.UNKNOWN
            else -> BiometricStatus.UNKNOWN
        }
    }
    
    /**
     * 顯示生物識別認證對話框
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "生物識別認證",
        subtitle: String = "使用您的指紋或臉部來驗證身份",
        negativeButtonText: String = "取消",
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }
                
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * 獲取生物識別狀態描述
     */
    fun getBiometricStatusDescription(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.AVAILABLE -> "生物識別認證可用"
            BiometricStatus.NOT_AVAILABLE -> "設備不支援生物識別認證"
            BiometricStatus.TEMPORARILY_NOT_AVAILABLE -> "生物識別認證暫時不可用"
            BiometricStatus.NOT_ENROLLED -> "尚未設定生物識別認證"
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "需要安全更新"
            BiometricStatus.UNKNOWN -> "生物識別狀態未知"
        }
    }
}

/**
 * 生物識別認證狀態
 */
enum class BiometricStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    TEMPORARILY_NOT_AVAILABLE,
    NOT_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNKNOWN
}
