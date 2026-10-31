plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.parallelc.micts"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.valohalo.samsungcts"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
    }
    buildTypes {
        debug {
            // Strip unused SDK backends in every APK, including CI debug artifacts.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.shizuku.api)
}
