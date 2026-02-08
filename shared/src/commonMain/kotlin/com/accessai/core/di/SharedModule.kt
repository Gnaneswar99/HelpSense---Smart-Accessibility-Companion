package com.accessai.core.di

import com.accessai.feature.audio.ClassifySoundUseCase
import com.accessai.feature.audio.MonitorSoundsUseCase
import com.accessai.feature.vision.CaptionImageUseCase
import com.accessai.feature.vision.ContinuousCaptioningUseCase
import com.accessai.feature.vision.DetectObjectsUseCase
import com.accessai.feature.vision.ExtractTextUseCase
import com.accessai.feature.navigation.DetectObstaclesUseCase
import com.accessai.feature.navigation.GetAccessibleRouteUseCase
import com.accessai.feature.navigation.MonitorObstaclesUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared Koin module containing all Use Cases.
 * Repository implementations are provided by platform-specific modules.
 */
val sharedModule: Module = module {

    // ── Vision Use Cases ──
    factory { CaptionImageUseCase(get()) }
    factory { DetectObjectsUseCase(get()) }
    factory { ExtractTextUseCase(get()) }
    factory { ContinuousCaptioningUseCase(get()) }

    // ── Audio Use Cases ──
    factory { MonitorSoundsUseCase(get(), get()) }
    factory { ClassifySoundUseCase(get()) }

    // ── Navigation Use Cases ──
    factory { GetAccessibleRouteUseCase(get()) }
    factory { DetectObstaclesUseCase(get()) }
    factory { MonitorObstaclesUseCase(get()) }
}

/**
 * Collects all shared modules.
 * Platform modules should add their own repository implementations.
 */
fun getSharedModules(): List<Module> = listOf(sharedModule)
