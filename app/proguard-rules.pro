# ====================================================================
# ProGuard / R8 Rules — SAMOSA (Sampah Otomatis Angsau)
# ====================================================================
# File ini HANYA berlaku untuk build RELEASE (isMinifyEnabled = true).
# Tujuan: obfuscation, shrinking, dan hardening keamanan APK.
# ====================================================================

# ------------------------------------------------------------------
# [FIX I1] STRIP SEMUA Log.* DARI RELEASE APK
# ------------------------------------------------------------------
# Menghapus semua panggilan android.util.Log dari bytecode release,
# termasuk dari library (Firebase, HiveMQ, dll).
# Ini mengatasi temuan MobSF: "The App logs information."
# ------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# ------------------------------------------------------------------
# FIREBASE — Keep rules agar Firebase tidak crash saat obfuscation
# ------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase Auth internal activities (M2, M3 di MobSF)
-keep class com.google.firebase.auth.internal.** { *; }

# Firebase Realtime Database — model classes
-keepclassmembers class com.example.sdn4angsau.samosa.TempatSampah {
    *;
}
-keepclassmembers class com.example.sdn4angsau.samosa.LogItem {
    *;
}

# ------------------------------------------------------------------
# HIVEMQ MQTT CLIENT — Keep rules
# ------------------------------------------------------------------
-keep class com.hivemq.client.** { *; }
-dontwarn com.hivemq.client.**

# Netty (dependency HiveMQ)
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# ------------------------------------------------------------------
# GSON — Keep rules agar serialisasi/deserialisasi tidak gagal
# ------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ------------------------------------------------------------------
# ANDROIDX SECURITY CRYPTO — EncryptedSharedPreferences
# ------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ------------------------------------------------------------------
# ANDROIDX — Keep rules umum
# ------------------------------------------------------------------
-keep class androidx.core.app.NotificationCompat** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ------------------------------------------------------------------
# KOTLIN COROUTINES
# ------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------
# UMUM — Preserve line numbers untuk crash reporting
# ------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile