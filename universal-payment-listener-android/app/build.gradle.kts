plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.universal.paymentlistener"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.universal.paymentlistener"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.work:work-runtime:2.11.2")
    testImplementation("junit:junit:4.13.2")
}
