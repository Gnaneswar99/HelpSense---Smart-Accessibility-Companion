package com.accessai.android.di

import com.accessai.android.ml.AndroidAudioRepository
import com.accessai.android.ml.AndroidNavigationRepository
import com.accessai.android.ml.AndroidVisionRepository
import com.accessai.android.ml.TFLiteImageCaptionEngine
import com.accessai.android.ml.TFLiteModelManager
import com.accessai.android.ml.TFLiteObjectDetectionEngine
import com.accessai.android.ml.TFLiteSoundClassificationEngine
import com.accessai.android.service.AndroidAudioCaptureService
import com.accessai.android.service.AndroidCameraService
import com.accessai.android.service.AndroidHapticService
import com.accessai.android.service.AndroidTextToSpeechService
import com.accessai.android.service.AndroidSpeechRecognitionService
import com.accessai.android.ui.screens.audio.AudioViewModel
import com.accessai.android.ui.screens.communication.CommunicationViewModel
import com.accessai.android.ui.screens.home.HomeViewModel
import com.accessai.android.ui.screens.navigation.NavigationViewModel
import com.accessai.android.ui.screens.vision.VisionViewModel
import com.accessai.core.ml.ImageCaptionEngine
import com.accessai.core.ml.ObjectDetectionEngine
import com.accessai.core.ml.SoundClassificationEngine
import com.accessai.core.model.AudioRepository
import com.accessai.core.model.NavigationRepository
import com.accessai.core.model.VisionRepository
import com.accessai.core.util.AudioCaptureService
import com.accessai.core.util.CameraService
import com.accessai.core.util.HapticService
import com.accessai.core.util.TextToSpeechService
import com.accessai.feature.navigation.MonitorObstaclesUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 * Provides platform services, ML engines, repositories, and ViewModels.
 */
val androidModule = module {

    // ── Core Accessibility Services ──
    single<TextToSpeechService> { AndroidTextToSpeechService(androidContext()) }
    single<HapticService> { AndroidHapticService(androidContext()) }

    // ── Camera & Audio Services ──
    single { AndroidCameraService(androidContext()) }
    single<CameraService> { get<AndroidCameraService>() }
    single<AudioCaptureService> { AndroidAudioCaptureService(androidContext()) }
    single { AndroidSpeechRecognitionService(androidContext()) }

    // ── ML Model Manager ──
    single { TFLiteModelManager(androidContext()) }

    // ── ML Engines ──
    single { TFLiteObjectDetectionEngine(androidContext(), get()) }
    single { TFLiteImageCaptionEngine(androidContext(), get(), get()) }
    single { TFLiteSoundClassificationEngine(androidContext(), get()) }
    single<ImageCaptionEngine> { get<TFLiteImageCaptionEngine>() }
    single<ObjectDetectionEngine> { get<TFLiteObjectDetectionEngine>() }
    single<SoundClassificationEngine> { get<TFLiteSoundClassificationEngine>() }

    // ── Repositories ──
    single<VisionRepository> { AndroidVisionRepository(get(), get(), get()) }
    single<AudioRepository> { AndroidAudioRepository(get(), get()) }
    single<NavigationRepository> { AndroidNavigationRepository(get()) }

    // ── ViewModels ──
    viewModel { HomeViewModel() }
    viewModel { VisionViewModel(get<AndroidCameraService>(), get(), get(), get()) }
    viewModel { AudioViewModel(get(), get(), get(), get()) }
    viewModel { NavigationViewModel(get(), get<AndroidCameraService>(), get(), get(), get()) }
    viewModel { CommunicationViewModel(get(), get(), get()) }
}
