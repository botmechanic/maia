import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            // AGP 9 registers the Kotlin extension with the Android application plugin.
            // Applying org.jetbrains.kotlin.android again double-registers `kotlin`.
            configureAndroidApplication()
        }
    }
}
