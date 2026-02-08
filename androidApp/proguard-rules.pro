# AccessAI ProGuard Rules

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Koin
-keep class org.koin.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }

# Keep data classes
-keep class com.accessai.core.model.** { *; }
