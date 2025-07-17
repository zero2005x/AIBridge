package com.aibridge.chat.utils

/**
 * 通用資源包裝類，用於處理異步操作的不同狀態
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

/**
 * 網路請求結果包裝類
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    data class Loading<T>(val isLoading: Boolean = true) : NetworkResult<T>()
    data class Exception<T>(val exception: Throwable) : NetworkResult<T>()
}

/**
 * 擴展函數：將 Resource 轉換為 NetworkResult
 */
fun <T> Resource<T>.toNetworkResult(): NetworkResult<T> {
    return when (this) {
        is Resource.Success -> NetworkResult.Success(data!!)
        is Resource.Error -> NetworkResult.Error(message ?: "Unknown error")
        is Resource.Loading -> NetworkResult.Loading()
    }
}

/**
 * 擴展函數：安全地處理 NetworkResult
 */
inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (String, Int?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) {
        action(message, code)
    }
    return this
}

inline fun <T> NetworkResult<T>.onLoading(action: (Boolean) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Loading) {
        action(isLoading)
    }
    return this
}

inline fun <T> NetworkResult<T>.onException(action: (Throwable) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Exception) {
        action(exception)
    }
    return this
}
