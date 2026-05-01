package com.example.leafai.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.leafai.model.LeafResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Wrapper around TensorFlow Lite for running leaf classification inference.
 * Handles model loading, input preprocessing, and output parsing.
 */
class MlWrapper(private val context: Context) {

    companion object {
        private const val TAG = "MlWrapper"
        private const val INPUT_SIZE = 224
        private const val MAX_LABELS = 10
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }

    private var interpreter: Interpreter? = null
    private var modelBuffer: MappedByteBuffer? = null
    private var isLoaded = false

    /**
     * Loads a TensorFlow Lite model from the given file path (URI or absolute path).
     */
    fun loadModel(modelPath: String): Boolean {
        return try {
            closeModel()

            val inputStream = FileInputStream(modelPath).channel
            modelBuffer = inputStream.map(FileChannel.MapMode.READ_ONLY, 0, inputStream.size())

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(modelBuffer as ByteBuffer, options)
            isLoaded = true

            Log.d(TAG, "Model loaded successfully from: $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isLoaded = false
            false
        }
    }

    /**
     * Runs inference on a bitmap image and returns the prediction results.
     */
    fun analyzeLeaf(bitmap: Bitmap): LeafResult {
        if (!isLoaded || interpreter == null) {
            return LeafResult.noModelLoaded()
        }

        return try {
            // Preprocess the image
            val processedImage = preprocessImage(bitmap)

            // Prepare output tensor
            val outputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, MAX_LABELS),
                org.tensorflow.lite.DataType.FLOAT32
            )

            // Run inference
            interpreter!!.run(processedImage.buffer, outputBuffer.buffer)

            // Parse results
            val probabilities = outputBuffer.floatArray
            val bestIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[bestIndex]

            // Generate a meaningful result based on the model output
            val species = parseSpecies(probabilities, bestIndex)
            val diagnosis = generateDiagnosis(species, confidence)
            val suggestions = generateSuggestions(species, confidence)

            LeafResult(
                species = species,
                confidence = confidence,
                diagnosis = diagnosis,
                suggestions = suggestions
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            LeafResult(
                species = "Error",
                confidence = 0f,
                diagnosis = "Analysis failed: ${e.message}",
                suggestions = listOf("Try taking another photo with better lighting.")
            )
        }
    }

    /**
     * Preprocesses the input bitmap for the model.
     */
    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
            .build()

        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        return tensorImage
    }

    /**
     * Parses species from raw probabilities.
     * For a custom model, you would map indices to actual species names.
     */
    private fun parseSpecies(probabilities: FloatArray, bestIndex: Int): String {
        // If the model outputs actual class labels as text, try to read them
        // Otherwise, use a generic label format
        return try {
            val labels = FileUtil.loadLabels(context, "label_map.txt")
            if (bestIndex < labels.size) {
                labels[bestIndex]
            } else {
                "Class #$bestIndex"
            }
        } catch (e: Exception) {
            // No label file found — return a generic label with percentage
            val percentage = ((probabilities[bestIndex] * 100.0f).toInt())
            "Class #$bestIndex ($percentage%)"
        }
    }

    /**
     * Generates a human-readable diagnosis based on the prediction.
     */
    private fun generateDiagnosis(species: String, confidence: Float): String {
        val confidenceLevel = when {
            confidence > 0.9f -> "very high"
            confidence > 0.7f -> "high"
            confidence > 0.5f -> "moderate"
            else -> "low"
        }

        val percentage = String.format("%.1f", confidence * 100.0f)
        return "The leaf appears to belong to $species with $confidenceLevel confidence ($percentage%)."
    }

    /**
     * Generates care suggestions based on the species and confidence.
     */
    private fun generateSuggestions(species: String, confidence: Float): List<String> {
        if (confidence < 0.5f) {
            return listOf(
                "The identification confidence is low. Try taking a clearer photo.",
                "Ensure the leaf fills most of the frame.",
                "Make sure lighting is good and the leaf is in focus."
            )
        }

        return listOf(
            "Consider checking specific care guidelines for $species.",
            "Monitor for common pests and diseases.",
            "Ensure proper watering and sunlight based on the species needs."
        )
    }

    /**
     * Closes the model interpreter and releases resources.
     */
    fun closeModel() {
        interpreter?.close()
        interpreter = null
        modelBuffer = null
        isLoaded = false
        Log.d(TAG, "Model closed.")
    }

    /**
     * Checks if a model is currently loaded.
     */
    fun isModelLoaded(): Boolean = isLoaded
}
