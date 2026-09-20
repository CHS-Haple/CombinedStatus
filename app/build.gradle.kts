plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

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
        minSdk = 24
        targetSdk = 37
        versionCode = 26092101
        versionName = "0.0.1"

        buildConfigField("String", "BUILD_ID", "\"20260921-01\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug { }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
}
