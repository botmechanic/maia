import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * Enforces Maia module dependency rules:
 * - :feature:* may not depend on another :feature:* or :app
 * - :core:* may not depend on :feature:* or :app
 */
class ModuleDependencyCheckPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.gradle.projectsEvaluated {
            target.rootProject.subprojects.forEach { project ->
                val path = project.path
                val isFeature = path.startsWith(":feature:")
                val isCore = path.startsWith(":core:")
                if (!isFeature && !isCore) return@forEach

                project.configurations
                    .filter { it.isCanBeResolved || it.name.contains("Implementation", ignoreCase = true) }
                    .forEach { configuration ->
                        configuration.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
                            val depPath = dep.path
                            if (depPath == path) return@forEach
                            if (isFeature) {
                                require(!depPath.startsWith(":feature:")) {
                                    "Illegal dependency: $path → $depPath (features cannot depend on features)"
                                }
                                require(depPath != ":app") {
                                    "Illegal dependency: $path → :app (features cannot depend on app)"
                                }
                            }
                            if (isCore) {
                                require(!depPath.startsWith(":feature:")) {
                                    "Illegal dependency: $path → $depPath (core cannot depend on features)"
                                }
                                require(depPath != ":app") {
                                    "Illegal dependency: $path → :app (core cannot depend on app)"
                                }
                            }
                        }
                    }
            }
        }
    }
}
