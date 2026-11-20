plugins {
    alias(libs.plugins.maia.android.library)
}

android {
    namespace = "com.example.core.runtime"
}

dependencies {
    api(project(":core:contract"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.mediapipe.tasks.text)
    testImplementation(libs.junit)
}
