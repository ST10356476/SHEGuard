plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.iiest10356476.sheguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iiest10356476.sheguard"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {

    implementation("com.github.bumptech.glide:glide:4.15.1")

    //This is for Location Service, Don't take it out please
    implementation("com.google.android.gms:play-services-location:21.3.0")
    //Media Background accessibility
    implementation("androidx.media:media:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")

    implementation("androidx.camera:camera-core:1.4.2")
// CameraX core
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.guava:guava:33.4.8-android")

    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BOM
    implementation (platform("com.google.firebase:firebase-bom:34.1.0"))

    // Firebase Authentication
    implementation ("com.google.firebase:firebase-auth")

    // Firebase Firestore
    implementation ("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // ViewModel and LiveData
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")
    implementation ("androidx.fragment:fragment-ktx:1.8.9")

    // Biometric for future use
    implementation ("androidx.biometric:biometric:1.1.0")

    // Encrypted Shared Preferences
    implementation ("androidx.security:security-crypto:1.1.0")

    //JSON Handling
    implementation("com.google.code.gson:gson:2.13.1")

}