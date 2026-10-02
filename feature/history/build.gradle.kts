plugins {
    alias(libs.plugins.babystatus.android.feature)
}

dependencies {
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.androidx.paging.testing)
}
