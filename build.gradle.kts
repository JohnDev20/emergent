plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}
android {
    namespace = "com.johndev.casaemdia"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.johndev.casaemdia"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
}
