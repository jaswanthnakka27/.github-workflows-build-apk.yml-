plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.karen.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.karen.assistant"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "KAREN"
    }
}
