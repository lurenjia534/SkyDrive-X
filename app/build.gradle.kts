plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)   // ← 用 alias
    alias(libs.plugins.ksp)    // ← 用 alias
}

val appVersionName = "1.0"

android {
    namespace = "com.lurenjia534.skydrivex"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lurenjia534.skydrivex"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
    }
}

dependencies {
    val media3_version = "1.8.0"
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.service)
    ksp(libs.hilt.compiler)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.2.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.microsoft.identity.client:msal:7.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("com.eygraber:compose-placeholder-material3:1.0.12")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("androidx.room:room-runtime:2.8.2")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Media3 for video playback (Compose UI)
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui-compose:$media3_version")
    // 额外音频解码（如 FLAC/ALAC/OPUS 等）- 方案1：使用 Jellyfin 预构建的 FFmpeg 扩展（与 Media3 1.8.0 匹配，GPLv3）
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1")

}
