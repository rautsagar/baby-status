import com.android.build.api.dsl.ApplicationExtension
import com.sagar.babystatus.TARGET_SDK
import com.sagar.babystatus.addUnitTestDependencies
import com.sagar.babystatus.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
                testOptions.unitTests.isReturnDefaultValues = true
            }
            addUnitTestDependencies()
        }
    }
}
