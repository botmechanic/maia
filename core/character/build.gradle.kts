plugins {
    alias(libs.plugins.maia.android.library)
}

android {
    namespace = "com.example.core.character"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
