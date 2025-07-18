package com.aibridge.chat.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aibridge.chat.data.api.AuthApiService
import com.aibridge.chat.data.api.ChatApiService
import com.aibridge.chat.data.api.AiChatApiService
import com.aibridge.chat.data.api.PortalApiService
import com.aibridge.chat.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.net.UnknownHostException
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * 簡單的記憶體Cookie存儲，確保session在不同服務間共享
     */
    private class MemoryCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<HttpUrl, MutableList<Cookie>>()
        
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val hostKey = HttpUrl.Builder()
                .scheme(url.scheme)
                .host(url.host)
                .port(url.port)
                .build()
            
            cookieStore.getOrPut(hostKey) { mutableListOf() }.apply {
                // 移除同名cookie，添加新的
                removeAll { existing -> cookies.any { it.name == existing.name } }
                addAll(cookies)
            }
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val hostKey = HttpUrl.Builder()
                .scheme(url.scheme)
                .host(url.host)
                .port(url.port)
                .build()
            
            return cookieStore[hostKey]?.filter { !it.expiresAt.let { time -> time != Long.MAX_VALUE && time < System.currentTimeMillis() } } ?: emptyList()
        }
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Custom interceptor for better error handling
        val networkErrorInterceptor = Interceptor { chain ->
            try {
                val request = chain.request()
                Log.d("NetworkModule", "Making request to: ${request.url}")
                
                val response = chain.proceed(request)
                Log.d("NetworkModule", "Response code: ${response.code}")
                response
            } catch (e: UnknownHostException) {
                Log.e("NetworkModule", "DNS resolution failed for ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e("NetworkModule", "Network request failed: ${e.message}", e)
                throw e
            }
        }
        
        return OkHttpClient.Builder()
            .cookieJar(MemoryCookieJar()) // 添加Cookie管理器
            .addInterceptor(loggingInterceptor)
            .addInterceptor(networkErrorInterceptor)
            .connectTimeout(45, TimeUnit.SECONDS) // Increased timeout
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Enable retry
            .followRedirects(true) // 啟用自動重定向以測試Portal API
            .followSslRedirects(true) // 啟用 SSL 重定向
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        // Use a placeholder base URL - actual URLs will be built dynamically in API services
        return Retrofit.Builder()
            .baseUrl("https://placeholder.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(okHttpClient: OkHttpClient, networkUtils: NetworkUtils, gson: Gson): AuthApiService {
        return AuthApiService(okHttpClient, networkUtils, gson)
    }
    
    @Provides
    @Singleton
    fun providePortalApiService(okHttpClient: OkHttpClient, networkUtils: NetworkUtils, gson: Gson): PortalApiService {
        return PortalApiService(okHttpClient, gson, networkUtils)
    }
    
    @Provides
    @Singleton
    fun provideAiChatApiService(
        okHttpClient: OkHttpClient, 
        networkUtils: NetworkUtils, 
        gson: Gson, 
        portalApiService: PortalApiService,
        authRepository: com.aibridge.chat.data.repository.AuthRepository
    ): AiChatApiService {
        return AiChatApiService(okHttpClient, networkUtils, gson, portalApiService, authRepository)
    }
    
    @Provides
    @Singleton
    fun provideBackendValidationApiService(okHttpClient: OkHttpClient, networkUtils: NetworkUtils, gson: Gson): com.aibridge.chat.data.api.BackendValidationApiService {
        return com.aibridge.chat.data.api.BackendValidationApiService(okHttpClient, networkUtils, gson)
    }
    
    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun providePortalDiscoveryService(okHttpClient: OkHttpClient, gson: Gson): com.aibridge.chat.data.api.PortalDiscoveryService {
        return com.aibridge.chat.data.api.PortalDiscoveryService(okHttpClient, gson)
    }
}
