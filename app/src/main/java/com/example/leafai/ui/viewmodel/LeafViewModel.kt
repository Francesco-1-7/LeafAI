package com.example.leafai.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafai.ml.MlWrapper
import com.example.leafai.model.LeafResult
import com.example.leafai.model.ModelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel that manages model loading state and leaf analysis logic.
 */
class LeafViewModel : ViewModel() {

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _lastResult = MutableStateFlow<LeafResult>(LeafResult.noModelLoaded())
    val lastResult: StateFlow<LeafResult> = _lastResult.asStateFlow()

    private lateinit var mlWrapper: MlWrapper

    fun init(context: Context) {
        mlWrapper = MlWrapper(context.applicationContext)
    }

    /**
     * Loads a TensorFlow Lite model from the given file path.
     */
    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading

            val success = withContext(Dispatchers.IO) {
                mlWrapper.loadModel(modelPath)
            }

            if (success) {
                _modelState.value = ModelState.Loaded(modelPath)
            } else {
                _modelState.value = ModelState.Error("Failed to load the model. Make sure it's a valid .tflite file.")
            }
        }
    }

    /**
     * Analyzes a leaf bitmap using the loaded model.
     */
    fun analyzeLeaf(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mlWrapper.analyzeLeaf(bitmap)
            }
            _lastResult.value = result
        }
    }

    /**
     * Clears the loaded model and resets state.
     */
    fun clearModel() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mlWrapper.closeModel()
            }
            _modelState.value = ModelState.Idle
            _lastResult.value = LeafResult.noModelLoaded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mlWrapper.closeModel()
    }
}
