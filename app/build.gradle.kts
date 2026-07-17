import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is driven by app/keystore.properties, which is gitignored so the keystore and its
// passwords never enter version control. When the file is absent (CI, fresh clones) the release
// build stays unsigned instead of failing.
val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "dev.koukeneko.essentialkeytools"
    compileSdk {
        // AndroidX core 1.19.0 and lifecycle 2.11.0 (already pinned in libs.versions.toml)
        // require compiling against API 37; building against 36.1 fails checkDebugAarMetadata.
        // targetSdk stays at 36 — compileSdk and targetSdk are independent.
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.koukeneko.essentialkeytools"
        minSdk = 35
        targetSdk = 36
        // CI derives these from the release tag and passes them as -PversionCode / -PversionName
        // (see release.yml); the literals below are only the fallback for local builds.
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 10007
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            // Preview builds install beside the Play/release app and are clearly labelled, so the
            // user can test CI artifacts without replacing their production installation.
            applicationIdSuffix = ".preview"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    androidResources {
        // Generate the per-app locale config from the values-* folders so the app appears in the
        // system per-app language settings; the default locale is declared in resources.properties.
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    // org.json ships with the Android runtime, so it is only needed on the JVM unit-test classpath
    // to exercise GitHubContributorsParser without a device.
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
