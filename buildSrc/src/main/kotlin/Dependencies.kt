/**
 * AccessAI - Centralized Dependency Management
 * All versions and dependencies in one place for consistency across modules.
 */
object Versions {
    // Kotlin & KMM
    const val kotlin = "1.9.22"
    const val kotlinCoroutines = "1.7.3"
    const val kotlinSerialization = "1.6.2"
    const val kmp = "1.9.22"

    // Android
    const val compileSdk = 34
    const val minSdk = 26
    const val targetSdk = 34
    const val agp = "8.2.2"
    const val composeBom = "2024.02.00"
    const val composeCompiler = "1.5.8"
    const val activityCompose = "1.8.2"
    const val navigationCompose = "2.7.7"
    const val lifecycleCompose = "2.7.0"

    // ML
    const val tensorflowLite = "2.14.0"
    const val tensorflowLiteGpu = "2.14.0"
    const val tensorflowLiteSupport = "0.4.4"

    // Camera & Media
    const val cameraX = "1.3.1"

    // DI
    const val koin = "3.5.3"
    const val koinCompose = "3.5.3"

    // Networking
    const val ktor = "2.3.7"

    // Database
    const val sqlDelight = "2.0.1"

    // Testing
    const val junit = "5.10.1"
    const val mockk = "1.13.9"
    const val turbine = "1.0.0"
}

object Deps {

    object Kotlin {
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}"
        const val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:0.5.0"
    }

    object AndroidX {
        const val composeBom = "androidx.compose:compose-bom:${Versions.composeBom}"
        const val composeUi = "androidx.compose.ui:ui"
        const val composeMaterial3 = "androidx.compose.material3:material3"
        const val composeTooling = "androidx.compose.ui:ui-tooling-preview"
        const val composeToolingDebug = "androidx.compose.ui:ui-tooling"
        const val activityCompose = "androidx.activity:activity-compose:${Versions.activityCompose}"
        const val navigationCompose = "androidx.navigation:navigation-compose:${Versions.navigationCompose}"
        const val lifecycleCompose = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycleCompose}"
        const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycleCompose}"
    }

    object CameraX {
        const val core = "androidx.camera:camera-core:${Versions.cameraX}"
        const val camera2 = "androidx.camera:camera-camera2:${Versions.cameraX}"
        const val lifecycle = "androidx.camera:camera-lifecycle:${Versions.cameraX}"
        const val view = "androidx.camera:camera-view:${Versions.cameraX}"
    }

    object ML {
        const val tensorflowLite = "org.tensorflow:tensorflow-lite:${Versions.tensorflowLite}"
        const val tensorflowLiteGpu = "org.tensorflow:tensorflow-lite-gpu:${Versions.tensorflowLiteGpu}"
        const val tensorflowLiteSupport = "org.tensorflow:tensorflow-lite-support:${Versions.tensorflowLiteSupport}"
    }

    object Koin {
        const val core = "io.insert-koin:koin-core:${Versions.koin}"
        const val android = "io.insert-koin:koin-android:${Versions.koin}"
        const val compose = "io.insert-koin:koin-androidx-compose:${Versions.koinCompose}"
    }

    object Ktor {
        const val core = "io.ktor:ktor-client-core:${Versions.ktor}"
        const val android = "io.ktor:ktor-client-okhttp:${Versions.ktor}"
        const val ios = "io.ktor:ktor-client-darwin:${Versions.ktor}"
        const val contentNegotiation = "io.ktor:ktor-client-content-negotiation:${Versions.ktor}"
        const val serialization = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"
        const val logging = "io.ktor:ktor-client-logging:${Versions.ktor}"
    }

    object SQLDelight {
        const val runtime = "app.cash.sqldelight:runtime:${Versions.sqlDelight}"
        const val androidDriver = "app.cash.sqldelight:android-driver:${Versions.sqlDelight}"
        const val nativeDriver = "app.cash.sqldelight:native-driver:${Versions.sqlDelight}"
        const val coroutines = "app.cash.sqldelight:coroutines-extensions:${Versions.sqlDelight}"
    }

    object Test {
        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
        const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinCoroutines}"
        const val mockk = "io.mockk:mockk:${Versions.mockk}"
        const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
    }
}
