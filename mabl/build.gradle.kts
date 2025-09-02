plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.penumbraos.mabl"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.penumbraos.mabl"
        minSdk = 32
        targetSdk = 36
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName") as String? ?: "1.0"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
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
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    "aipinImplementation"(libs.moonlight.ui)
    "aipinSimulatorImplementation"(libs.moonlight.ui)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.onnx.runtime.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    debugImplementation(libs.ui.tooling)
    ksp(libs.androidx.room.compiler)
}