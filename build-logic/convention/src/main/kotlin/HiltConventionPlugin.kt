import com.sagar.babystatus.lib
import com.sagar.babystatus.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            dependencies.add("ksp", libs.lib("hilt-compiler"))
            pluginManager.withPlugin("com.android.base") {
                pluginManager.apply("com.google.dagger.hilt.android")
                dependencies.add("implementation", libs.lib("hilt-android"))
            }
        }
    }
}
