plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The access code for Google Play's app reviewers, never stored on GitHub. Put it in local.properties
// (or in your user gradle.properties), for example:  reviewCode=CALM2026
// Letters and digits only. Empty = the code button is hidden.
val reviewCode: String = (
    (findProperty("reviewCode") as String?)
        ?: rootProject.file("local.properties").takeIf { it.exists() }
            ?.readLines()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("reviewCode=") }
            ?.substringAfter("=")
    ).orEmpty().filter { it.isLetterOrDigit() }

logger.lifecycle(
    if (reviewCode.isEmpty()) "Review code: not set (the access code button is hidden)"
    else "Review code: set (${reviewCode.length} characters)",
)

android {
    namespace = "com.newjourney.core.billing"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "REVIEW_CODE", "\"$reviewCode\"")
    }
    // BuildConfig.DEBUG decides whether the developer-only Premium preview exists.
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(libs.play.billing)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
