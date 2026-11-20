plugins {
    alias(libs.plugins.maia.android.library)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.core.memory"
}

dependencies {
    api(project(":core:contract"))
    api(project(":core:database"))
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
}
