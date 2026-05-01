package com.example.leafai.model

/**
 * Represents the current state of the loaded ML model.
 */
sealed class ModelState {
    object Idle : ModelState()
    object Loading : ModelState()
    data class Loaded(val modelPath: String) : ModelState()
    data class Error(val message: String) : ModelState()
}
