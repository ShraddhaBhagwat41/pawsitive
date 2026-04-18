plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.pawsitive.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pawsitive.app"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // OkHttp for network requests
    implementation(libs.okhttp)

    // Security Crypto for EncryptedSharedPreferences
    implementation(libs.security.crypto)

    // Firebase BoM and products
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Play Services Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    // Glide for image loading
    implementation(libs.glide)

    // Retrofit for REST API
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)

    // FCM and WorkManager for notifications and background tasks
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("androidx.work:work-runtime:2.9.0")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}