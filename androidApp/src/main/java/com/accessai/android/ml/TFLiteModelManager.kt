package com.accessai.android.ml

import android.content.Context
import com.accessai.core.ml.MLEngine
import com.accessai.core.ml.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Manages TensorFlow Lite model loading and lifecycle.
 * Uses CPU with 4 threads for efficient on-device inference.
 */
class TFLiteModelManager(
    private val context: Context
) : MLEngine {

    private val interpreters = mutableMapOf<String, Interpreter>()
    private val modelInfoMap = mutableMapOf<String, ModelInfo>()
    private var _isReady = false

    companion object {
        const val MODEL_OBJECT_DETECTION = "detect.tflite"
        const val MODEL_IMAGE_LABELING = "label.tflite"
        const val MODEL_SOUND_CLASSIFICATION = "yamnet.tflite"
        const val MODEL_OCR = "ocr.tflite"

        const val LABELS_OBJECT_DETECTION = "labelmap.txt"
        const val LABELS_IMAGE = "labels.txt"
        const val LABELS_SOUND = "yamnet_labels.txt"

        const val NUM_THREADS = 4
    }

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            _isReady = true
            true
        } catch (e: Exception) {
            _isReady = false
            false
        }
    }

    suspend fun loadModel(
        modelName: String,
        useGpu: Boolean = false
    ): Interpreter? = withContext(Dispatchers.IO) {
        try {
            interpreters[modelName]?.let { return@withContext it }

            val modelBuffer = loadModelFile(modelName)
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
            }

            val interpreter = Interpreter(modelBuffer, options)
            interpreters[modelName] = interpreter

            val inputTensor = interpreter.getInputTensor(0)
            modelInfoMap[modelName] = ModelInfo(
                name = modelName,
                version = "1.0",
                sizeBytes = modelBuffer.capacity().toLong(),
                inputShape = inputTensor.shape().toList(),
                isLoaded = true
            )

            interpreter
        } catch (e: Exception) {
            null
        }
    }

    fun getInterpreter(modelName: String): Interpreter? = interpreters[modelName]

    fun getModelInfo(modelName: String): ModelInfo? = modelInfoMap[modelName]

    fun getLoadedModels(): List<String> = interpreters.keys.toList()

    fun loadLabels(labelsName: String): List<String> {
        return try {
            context.assets.open("models/$labelsName")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("models/$modelName")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    override fun isReady(): Boolean = _isReady

    override fun release() {
        interpreters.values.forEach { it.close() }
        interpreters.clear()
        modelInfoMap.clear()
        _isReady = false
    }
}
