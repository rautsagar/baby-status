plugins {
    alias(libs.plugins.babystatus.android.library)
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
    api(libs.junit)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(projects.core.common)
}
