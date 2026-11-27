plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mytv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mytv.home"
        minSdk = 24
        targetSdk = 33
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("my-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
