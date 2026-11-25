import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Optional build-time AI endpoint config. Never committed (see .gitignore).
// secrets.properties keys: AI_BASE_URL, AI_API_KEY, AI_MODEL. Env vars with the same names win.
val secrets = Properties().apply {
    val f = rootProject.file("secrets.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String, default: String = ""): String =
    (System.getenv(name) ?: secrets.getProperty(name) ?: default).replace("\"", "")

android {
    namespace = "com.aircrushin.mindatlas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aircrushin.mindatlas"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "AI_BASE_URL", "\"${secret("AI_BASE_URL", "https://openrouter.ai/api/v1")}\"")
        buildConfigField("String", "AI_API_KEY", "\"${secret("AI_API_KEY")}\"")
        buildConfigField("String", "AI_MODEL", "\"${secret("AI_MODEL", "deepseek/deepseek-chat-v3-0324")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Debug keystore so the judge APK is installable; replace with a real key for store release.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.spatial.bom))
    implementation(libs.spatial.core)
    implementation(libs.spatial.ui.platform)
    implementation(libs.spatial.ui.foundation)
    implementation(libs.spatial.ui.design)
    testImplementation(libs.junit)
}

// Same exclusions as the official PICO samples: the Spatial UI ships its own forks of these.
configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}
