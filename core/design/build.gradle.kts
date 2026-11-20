plugins {
    alias(libs.plugins.maia.android.library)
    alias(libs.plugins.maia.android.compose)
}

android {
    namespace = "com.example.core.design"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
}
