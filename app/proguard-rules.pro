# ProGuard / R8 Rules for Maia Companion

# Preserve line numbers and file names for debugging crash traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Native methods (JNI entry points)
-keepclasseswithmembernames class * {
    native <methods>;
}

# SQLCipher 16KB-aligned database encryption
-keep class net.zetetic.** { *; }
-keep interface net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }

# MediaPipe GenAI & Text / LiteRT
-keep class com.google.mediapipe.** { *; }
-keep interface com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# TensorFlow Lite & USE Lite
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-dontwarn androidx.room.**

# WorkManager workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
