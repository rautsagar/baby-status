import java.util.Properties

plugins {
    alias(libs.plugins.babystatus.android.application.compose)
    alias(libs.plugins.babystatus.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.sagar.babystatus"

    defaultConfig {
        applicationId = "com.sagar.babystatus"
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String",
            "FEEDBACK_EMAIL",
            "\"${localProperties.getProperty("feedback.email", "")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(projects.feature.growth)
    implementation(projects.feature.history)
    implementation(projects.feature.log)
    implementation(projects.feature.profile)
    implementation(projects.feature.pump)
    implementation(projects.feature.settings)
    implementation(projects.feature.today)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
