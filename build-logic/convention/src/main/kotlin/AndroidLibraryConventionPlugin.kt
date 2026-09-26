import com.android.build.api.dsl.LibraryExtension
import com.sagar.babystatus.addUnitTestDependencies
import com.sagar.babystatus.configureKotlinAndroid
import com.sagar.babystatus.defaultNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                namespace = defaultNamespace
                configureKotlinAndroid(this)
                testOptions.unitTests.isReturnDefaultValues = true
            }
            addUnitTestDependencies()
        }
    }
}
