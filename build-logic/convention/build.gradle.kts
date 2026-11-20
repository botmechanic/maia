plugins {
    `kotlin-dsl`
}

group = "com.example.maia.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "maia.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "maia.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "maia.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "maia.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("moduleDependencyCheck") {
            id = "maia.module.dependency.check"
            implementationClass = "ModuleDependencyCheckPlugin"
        }
    }
}
