plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.sdn4angsau.samosa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sdn4angsau.samosa"
        // [FIX H2] Naikkan minSdk ke 26 (Android 8.0 Oreo)
        // Android 7.0 (API 24) memiliki kerentanan keamanan yang tidak lagi dipatch.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // [FIX I-1] Aktifkan ProGuard/R8 untuk obfuscation — mempersulit reverse engineering APK
            isMinifyEnabled = true
            isShrinkResources = true
            // [FIX H3] Pastikan release build TIDAK debuggable
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // H3 & H4: debuggable=true dan testOnly=true HANYA berlaku di debug build.
            // MobSF mendeteksi ini karena APK yang discan adalah debug build.
            // Release build secara default sudah aman (debuggable=false, testOnly=false).
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    
    // IoT & Security Best Practices
    implementation(libs.hivemq.mqtt.client)
    implementation(libs.androidx.security.crypto)
    implementation(libs.gson)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
