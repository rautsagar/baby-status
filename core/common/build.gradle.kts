plugins {
    alias(libs.plugins.babystatus.android.library)
    alias(libs.plugins.babystatus.hilt)
}

dependencies {
    api(libs.kotlinx.coroutines.android)
}
