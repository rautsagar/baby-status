plugins {
    alias(libs.plugins.babystatus.android.library.compose)
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}
