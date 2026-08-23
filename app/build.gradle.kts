import java.io.File
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Nomme l'APK final avec la version : lumen-horloge-v1.2.apk
tasks.register("renameApk") {
    dependsOn("assembleDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        if (!apk.exists()) {
            println("APK introuvable: ${apk.absolutePath}")
            return@doLast
        }
        val v = android.defaultConfig.versionName
        val target = File(apk.parentFile, "lumen-horloge-v${v}.apk")
        apk.copyTo(target, overwrite = true)
        println("APK renommé: ${target.absolutePath}")
    }
}

fun releaseKeystore(): File? {
    System.getenv("LUMEN_KEYSTORE_B64")?.let { b64 ->
        val tmp = File(System.getenv("RUNNER_TEMP") ?: "/tmp", "lumen-release.keystore")
        tmp.writeBytes(Base64.getDecoder().decode(b64))
        return tmp
    }
    val local = File(System.getProperty("user.home"), ".secrets/lumen-release.keystore")
    return if (local.exists()) local else null
}

android {
    namespace = "com.trucdecomptable.lumen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trucdecomptable.lumen"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
    }

    // Same pattern as cuisson-vapeur-legumes: release signed with a stable
    // keystore, debug unsigned. No keystore here → skip release config.
    val ks = releaseKeystore()
    if (ks != null) {
        signingConfigs {
            create("release") {
                storeFile = ks
                storePassword = System.getenv("LUMEN_KEYSTORE_PASSWORD") ?: "CHANGE_ME"
                keyAlias = System.getenv("LUMEN_KEY_ALIAS") ?: "lumen"
                keyPassword = System.getenv("LUMEN_KEY_PASSWORD") ?: "CHANGE_ME"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ks?.let { signingConfig = signingConfigs.getByName("release") }
        }
        debug {
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-tooling")
}
