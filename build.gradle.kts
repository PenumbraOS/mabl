// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

tasks {
    /**
     * Gradle task to install the demo plugin's APK onto connected devices.
     * Requires 'adb' to be in the system's PATH or specified with a full path
     */
    register<Exec>("installDemoPlugins") {
        description = "Installs the demo plugin's APK on connected device(s) using adb"
        group = "install"

        val serviceProject = project(":plugins:demo")

        val serviceApkPath =
            "${serviceProject.buildDir}/outputs/apk/debug/${serviceProject.name}-debug.apk"

        commandLine("bash", "-c", "adb install -r $serviceApkPath")

        onlyIf {
            File(serviceApkPath).exists()
        }

        dependsOn(":plugins:demo:assembleDebug")
    }
}