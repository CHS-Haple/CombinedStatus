plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val combinedStatusVersionName = providers.gradleProperty("combinedStatus.versionName").get()
val combinedStatusVersionCode = providers.gradleProperty("combinedStatus.versionCode").get().toInt()
val combinedStatusBuildId = providers.gradleProperty("combinedStatus.buildId").get()

val hapleKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val hapleKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val hapleKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val hapleKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hapleSigningEnabled =
    !hapleKeystorePath.isNullOrBlank() &&
        !hapleKeystorePassword.isNullOrBlank() &&
        !hapleKeyAlias.isNullOrBlank() &&
        !hapleKeyPassword.isNullOrBlank() &&
        file(hapleKeystorePath).isFile

@Suppress("UnstableApiUsage")
android {
    namespace = "com.chaners.combinedstatus"
    buildToolsVersion = "37.0.0"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.chaners.combinedstatus"
        minSdk = 33
        targetSdk = 37
        versionCode = combinedStatusVersionCode
        versionName = combinedStatusVersionName

        buildConfigField("String", "BUILD_ID", "\"$combinedStatusBuildId\"")
    }

    signingConfigs {
        if (hapleSigningEnabled) {
            create("haple") {
                storeFile = file(requireNotNull(hapleKeystorePath))
                storePassword = hapleKeystorePassword
                keyAlias = hapleKeyAlias
                keyPassword = hapleKeyPassword

                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
}
