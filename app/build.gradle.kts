plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
}

android {
    namespace = "com.yurisa.rwdps4"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yurisa.rwdps4"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        renderscriptTargetApi = 21
        renderscriptSupportModeEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(files("golang_aar/proxy.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation("androidx.palette:palette:1.0.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")
}