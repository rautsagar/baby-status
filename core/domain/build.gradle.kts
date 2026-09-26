plugins {
    alias(libs.plugins.babystatus.android.library)
}

dependencies {
    api(projects.core.data)
    implementation(projects.core.common)
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}
