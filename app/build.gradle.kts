plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yurial.abrakazyak"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yurial.abrakazyak"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            // PLACEHOLDER SIGNING: the release build is signed with the debug key
            // so the CI release workflow can produce an installable APK without
            // a real keystore. A production keystore (upload key) is a separate
            // task tracked in TODO.md; do not ship to stores with this setting.
            signingConfig = signingConfigs.getByName("debug")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        // java.time and java.nio.file from :core run on minSdk 24 via
        // core library desugaring.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.tools.desugar.jdk.libs)
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // AGP built-in Kotlin does not register the kotlin("test") shorthand:
    // the test framework is declared explicitly, backend junit4 for the
    // default AGP local-test runner.
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
