package com.example.leafai.model

/**
 * Holds the result of a leaf analysis prediction.
 */
data class LeafResult(
    val species: String,
    val confidence: Float,
    val diagnosis: String,
    val suggestions: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Creates a placeholder result for when no model is loaded.
         */
        fun noModelLoaded(): LeafResult {
            return LeafResult(
                species = "N/A",
                confidence = 0f,
                diagnosis = "No AI model loaded. Please upload a .tflite model first.",
                suggestions = emptyList()
            )
        }
    }
}
