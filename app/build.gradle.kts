plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.example.appscheduler" // change to your package
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appscheduler" // change to your package
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true            // enable ViewBinding for XML
        // dataBinding = true         // uncomment if you want DataBinding instead
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")


    val lifecycle_version = "2.7.0" // Check for the latest version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")

    // For the by viewModels() property delegate in Activities
    val activity_ktx_version = "1.9.0" // Check for the latest version
    implementation("androidx.activity:activity-ktx:$activity_ktx_version")

    implementation("androidx.work:work-runtime-ktx:2.8.1")   // or latest
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("com.google.android.material:material:1.10.0")

    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
}
