import androidx.room.gradle.RoomExtension
import com.sagar.babystatus.lib
import com.sagar.babystatus.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")

            // Exported schemas are committed so migrations stay reviewable.
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies.apply {
                add("implementation", libs.lib("room-runtime"))
                add("implementation", libs.lib("room-ktx"))
                add("ksp", libs.lib("room-compiler"))
            }
        }
    }
}
