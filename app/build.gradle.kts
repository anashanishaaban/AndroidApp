// Module-level (app) build.gradle.kts
import java.util.Properties


plugins {
    id("com.android.application")           // Apply the Android Application plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.campuseventsscheduler"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }


    defaultConfig {
        applicationId = "com.example.campuseventsscheduler"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Read from local.properties
        val properties = Properties()
        project.rootProject.file("local.properties").inputStream().use {
            properties.load(it)
        }

        buildConfigField(
            type = "String",
            name = "MAPS_API_KEY",
            value = properties.getProperty("MAPS_API_KEY")?.let { "\"$it\"" } ?: "\"\""
        )

        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: ""
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

    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Add Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.firestore)  // Example: Firebase Analytics

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
