import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            // Prefer Kotlin Android only when AGP did not already register it.
            try {
                configureAndroidLibrary()
            } catch (e: Exception) {
                pluginManager.apply("org.jetbrains.kotlin.android")
                configureAndroidLibrary()
            }
        }
    }
}
