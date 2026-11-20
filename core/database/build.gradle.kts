plugins {
    alias(libs.plugins.maia.android.library)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.core.database"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher)
    ksp(libs.androidx.room.compiler)
}
