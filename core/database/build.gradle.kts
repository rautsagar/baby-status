plugins {
    alias(libs.plugins.babystatus.android.library)
    alias(libs.plugins.babystatus.android.room)
    alias(libs.plugins.babystatus.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.room.paging)
}
