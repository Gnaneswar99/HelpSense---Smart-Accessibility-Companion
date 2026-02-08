package com.accessai.android.ml

import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.VisionRepository
import com.accessai.core.util.CameraService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Android implementation of VisionRepository.
 * Connects the TFLite ML engines to the shared domain layer.
 */
class AndroidVisionRepository(
    private val captionEngine: TFLiteImageCaptionEngine,
    private val objectDetectionEngine: TFLiteObjectDetectionEngine,
    private val cameraService: CameraService
) : VisionRepository {

    override suspend fun captionImage(imageBytes: ByteArray): InferenceResult<CaptionResult> {
        return captionEngine.caption(imageBytes)
    }

    override suspend fun detectObjects(imageBytes: ByteArray): InferenceResult<List<DetectedObject>> {
        return objectDetectionEngine.detect(imageBytes)
    }

    override suspend fun extractText(imageBytes: ByteArray): InferenceResult<String> {
        // OCR will be added in a later step
        return InferenceResult.Error("OCR not yet implemented")
    }

    /**
     * Continuous captioning: captures frames from the camera
     * and runs the captioning pipeline on each frame.
     */
    override fun continuousCaptioning(): Flow<InferenceResult<CaptionResult>> = flow {
        cameraService.frameStream().collect { frame ->
            val result = captionEngine.caption(frame.data)
            emit(result)
        }
    }
}
