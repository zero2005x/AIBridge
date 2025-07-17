# Add project specific ProGuard rules here.

# Keep all model classes
-keep class com.aibridge.chat.domain.model.** { *; }
-keep class com.aibridge.chat.data.database.entities.** { *; }
-keep class com.aibridge.chat.data.api.** { *; }

# Keep Retrofit interfaces
-keep interface com.aibridge.chat.data.api.** { *; }

# Keep Gson related classes
-keepattributes Signature
-keepattributes Annotation
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Keep Room related
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep Compose related
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep security related classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# General Android optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
