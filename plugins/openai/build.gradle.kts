plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.penumbraos.plugins.openai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.penumbraos.plugins.openai"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":sdk"))

    implementation(libs.penumbraos.sdk)

    implementation(libs.openai.client)
    implementation(libs.ktor.client.android)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}