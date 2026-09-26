plugins {
    alias(libs.plugins.babystatus.android.library.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    api(libs.coil.compose)
}
