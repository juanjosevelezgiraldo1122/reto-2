plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localFinnhubToken = rootProject.file(".env").takeIf { it.isFile }
    ?.readLines()
    ?.firstOrNull { it.trimStart().startsWith("FINNHUB_API_KEY=") }
    ?.substringAfter('=')
    ?.trim()
    ?.removeSurrounding("\"")
    ?.removeSurrounding("'")
    .orEmpty()
val finnhubToken = providers.environmentVariable("FINNHUB_API_KEY").orElse(localFinnhubToken).get()

android {
    namespace = "com.example.reto2"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.reto2"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "FINNHUB_TOKEN", "\"$finnhubToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
