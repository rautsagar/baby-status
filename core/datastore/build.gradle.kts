plugins {
    alias(libs.plugins.babystatus.android.library)
    alias(libs.plugins.babystatus.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore.preferences)
}
