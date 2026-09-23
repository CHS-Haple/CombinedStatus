plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val combinedStatusVersionName = providers.gradleProperty("combinedStatus.versionName").get()
val combinedStatusVersionCode = providers.gradleProperty("combinedStatus.versionCode").get().toInt()
val combinedStatusBuildId = providers.gradleProperty("combinedStatus.buildId").get()
val miuixVersion = providers.gradleProperty("miuix.version").get()
val miuixRevision = providers.gradleProperty("miuix.revision").get()

val hapleKeystorePath = providers.environmentVariable("HAPLE_KEYSTORE_PATH").orNull
val hapleKeystorePassword = providers.environmentVariable("HAPLE_KEYSTORE_PASSWORD").orNull
val hapleKeyAlias = providers.environmentVariable("HAPLE_KEY_ALIAS").orNull
val hapleSigningEnabled =
    !hapleKeystorePath.isNullOrBlank() &&
        !hapleKeystorePassword.isNullOrBlank() &&
        !hapleKeyAlias.isNullOrBlank() &&
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
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixVersion\"")
        buildConfigField("String", "MIUIX_REVISION", "\"$miuixRevision\"")
    }

    signingConfigs {
        if (hapleSigningEnabled) {
            create("haple") {
                storeFile = file(requireNotNull(hapleKeystorePath))
                storePassword = hapleKeystorePassword
                keyAlias = hapleKeyAlias
                keyPassword = hapleKeystorePassword

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
            buildConfigField("String", "BUILD_CHANNEL", "\"debug\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "true")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
        }
        release {
            buildConfigField("String", "BUILD_CHANNEL", "\"release\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "false")
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
        create("canary") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            buildConfigField("String", "BUILD_CHANNEL", "\"canary\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            // Modern Xposed metadata is loaded directly by the framework and must survive
            // optimized Canary/Release packaging even when dependency graphs change.
            merges += "META-INF/xposed/**"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")

    testImplementation("junit:junit:4.13.2")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
}
