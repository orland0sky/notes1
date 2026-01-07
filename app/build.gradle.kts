plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // GUNAKAN ALIAS agar otomatis mengambil versi 2.0.21-1.0.26 dari file TOML
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.project1"
    // Di tahun 2026, gunakan SDK 35 agar aplikasi lebih lancar di Android terbaru
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.project1"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 17 adalah standar untuk Android modern
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Menggunakan library dari libs.versions.toml
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Library Room untuk Database (SQLite Lokal)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    // KSP inilah yang memproses database kamu saat aplikasi di-build
    ksp("androidx.room:room-compiler:$room_version")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}