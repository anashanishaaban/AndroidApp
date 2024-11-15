// Module-level (app) build.gradle.kts

plugins {
    id("com.android.application")           // Apply the Android Application plugin
    id("com.google.gms.google-services")     // Apply the Google services plugin
}

android {
    namespace = "com.example.campuseventsscheduler"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.campuseventsscheduler"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation(libs.play.services.maps)

    // Firebase Authentication dependency
    implementation("com.google.firebase:firebase-auth")

    // Import the Firebase BoM to manage Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Add Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")  // Example: Firebase Analytics

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
