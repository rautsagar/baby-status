plugins {
    alias(libs.plugins.babystatus.android.library)
    alias(libs.plugins.babystatus.hilt)
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.paging.common)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(libs.androidx.core.ktx)
    implementation(libs.room.paging)

    testImplementation(projects.core.testing)
}
