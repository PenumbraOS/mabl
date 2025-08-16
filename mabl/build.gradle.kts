plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.penumbraos.mabl"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.penumbraos.mabl"
        minSdk = 32
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName") as String? ?: "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "device"
    productFlavors {
        create("aipin") {
            dimension = "device"
            applicationIdSuffix = ".pin"
            buildConfigField("boolean", "IS_AI_PIN", "true")
            buildConfigField("boolean", "IS_SIMULATOR", "false")
        }
        create("aipinSimulator") {
            dimension = "device"
            applicationIdSuffix = ".pinsim"
            buildConfigField("boolean", "IS_AI_PIN", "true")
            buildConfigField("boolean", "IS_SIMULATOR", "true")
        }
        create("android") {
            dimension = "device"
            applicationIdSuffix = ".android"
            buildConfigField("boolean", "IS_AI_PIN", "false")
            buildConfigField("boolean", "IS_SIMULATOR", "false")
        }
    }

    sourceSets {
        getByName("aipin") {
            java.srcDirs("src/aipincore/java")
        }
        getByName("aipinSimulator") {
            java.srcDirs("src/aipincore/java")
        }
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(libs.penumbraos.sdk)
    "aipinImplementation"(libs.moonlight.ui)
    "aipinSimulatorImplementation"(libs.moonlight.ui)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.onnx.runtime.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}