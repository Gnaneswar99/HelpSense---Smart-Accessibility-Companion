package com.accessai.feature.vision

import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.VisionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for generating image captions.
 * Encapsulates business logic for the Vision module.
 */
class CaptionImageUseCase(
    private val visionRepository: VisionRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): InferenceResult<CaptionResult> {
        if (imageBytes.isEmpty()) {
            return InferenceResult.Error("Image data is empty")
        }
        return visionRepository.captionImage(imageBytes)
    }
}

/**
 * Use case for detecting objects in images.
 */
class DetectObjectsUseCase(
    private val visionRepository: VisionRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): InferenceResult<List<DetectedObject>> {
        if (imageBytes.isEmpty()) {
            return InferenceResult.Error("Image data is empty")
        }
        return visionRepository.detectObjects(imageBytes)
    }
}

/**
 * Use case for extracting text from images (OCR).
 */
class ExtractTextUseCase(
    private val visionRepository: VisionRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): InferenceResult<String> {
        if (imageBytes.isEmpty()) {
            return InferenceResult.Error("Image data is empty")
        }
        return visionRepository.extractText(imageBytes)
    }
}

/**
 * Use case for continuous live captioning.
 */
class ContinuousCaptioningUseCase(
    private val visionRepository: VisionRepository
) {
    operator fun invoke(): Flow<InferenceResult<CaptionResult>> {
        return visionRepository.continuousCaptioning()
    }
}
