plugins {
    alias(libs.plugins.maia.android.library)
    alias(libs.plugins.maia.android.compose)
}

android {
    namespace = "com.example.feature.memoryview"
}

dependencies {
    api(project(":core:contract"))
    api(project(":core:model"))
    implementation(project(":core:design"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
