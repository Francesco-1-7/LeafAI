package com.example.leafai.viewmodel

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafai.ml.MlWrapper
import com.example.leafai.model.LeafResult
import com.example.leafai.model.ModelState
import com.example.leafai.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ViewModel that manages model loading state and leaf analysis logic.
 */
class LeafViewModel : ViewModel() {

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _lastResult = MutableStateFlow<LeafResult?>(null)
    val lastResult: StateFlow<LeafResult?> = _lastResult.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _currentPlantName = MutableStateFlow("")
    val currentPlantName: StateFlow<String> = _currentPlantName.asStateFlow()

    private lateinit var mlWrapper: MlWrapper

    fun init(context: Context) {
        if (!::mlWrapper.isInitialized) {
            synchronized(this) {
                if (!::mlWrapper.isInitialized) {
                    mlWrapper = MlWrapper(context.applicationContext)
                    // Try to auto-load the last used model
                    autoLoadLastModel(context.applicationContext)
                }
            }
        }
    }

    fun setPlantName(name: String) {
        _currentPlantName.value = name
    }

    private fun autoLoadLastModel(context: Context) {
        val prefs = context.getSharedPreferences("leaf_ai_prefs", Context.MODE_PRIVATE)
        val lastPath = prefs.getString("last_model_path", null)
        if (lastPath != null && File(lastPath).exists()) {
            loadModel(context, lastPath)
        }
    }

    /**
     * Loads a model from a Uri. If it's a content Uri, it copies it to local storage first.
     */
    fun loadModel(context: Context, uri: Uri) {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading
            try {
                val path = withContext(Dispatchers.IO) {
                    if (uri.scheme == "content") {
                        copyUriToInternalStorage(context, uri)
                    } else {
                        uri.path ?: throw IllegalArgumentException("Invalid URI path")
                    }
                }
                
                withContext(Dispatchers.IO) {
                    mlWrapper.loadModel(context, path)
                }
                
                // Save path for next time
                context.getSharedPreferences("leaf_ai_prefs", Context.MODE_PRIVATE)
                    .edit().putString("last_model_path", path).apply()
                
                _modelState.value = ModelState.Loaded(path)
            } catch (e: Exception) {
                _modelState.value = ModelState.Error("Failed to load Gemma 4: ${e.message}")
            }
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        val fileName = getFileName(context, uri) ?: "model.bin"
        val file = File(context.filesDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Could not open input stream from Uri")
        
        return file.absolutePath
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    /**
     * Loads a TensorFlow Lite model from the given absolute file path.
     * Required for Gemma 4 E2B models (> 2GB).
     */
    fun loadModel(context: Context, modelPath: String) {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading
            try {
                withContext(Dispatchers.IO) {
                    mlWrapper.loadModel(context, modelPath)
                }
                
                // Save path for next time
                context.getSharedPreferences("leaf_ai_prefs", Context.MODE_PRIVATE)
                    .edit().putString("last_model_path", modelPath).apply()

                // MediaPipe doesn't return success boolean on loadModel in our current wrapper, 
                // but throws if it fails. We'll assume success if no exception.
                _modelState.value = ModelState.Loaded(modelPath)
            } catch (e: Exception) {
                _modelState.value = ModelState.Error("Failed to load Gemma 4: ${e.message}")
            }
        }
    }

    /**
     * Analyzes a leaf bitmap using Gemma 4 E2B Vision.
     */
    fun analyzeLeaf(bitmap: Bitmap) {
        // Clear previous and set initial "Analyzing" state
        _lastResult.value = LeafResult("Analyzing...", 0f, "Processing image...", emptyList())
        _chatMessages.value = listOf(ChatMessage("Sto analizzando la tua foto, un momento...", isUser = false))
        _isSendingMessage.value = true

        viewModelScope.launch {
            try {
                val diagnosis = withContext(Dispatchers.IO) {
                    mlWrapper.analyzeLeaf(bitmap, _currentPlantName.value)
                }
                _lastResult.value = LeafResult(
                    species = "Leaf Identification",
                    confidence = 0.95f,
                    diagnosis = diagnosis,
                    suggestions = listOf(
                        "Ask for specific care instructions.",
                        "Ask about similar looking diseases."
                    )
                )
                // Replace "Analyzing" message with real diagnosis
                _chatMessages.value = listOf(
                    ChatMessage(diagnosis, isUser = false)
                )
            } catch (e: Exception) {
                _chatMessages.value = listOf(ChatMessage("Errore nell'analisi: ${e.message}", isUser = false))
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    /**
     * Sends a chat message to the model for follow-up questions.
     */
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(text, isUser = true)
        _chatMessages.value = _chatMessages.value + userMessage
        _isSendingMessage.value = true

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    mlWrapper.generateResponse(text)
                }
                _chatMessages.value = _chatMessages.value + ChatMessage(response, isUser = false)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage("Error: ${e.message}", isUser = false)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    /**
     * Clears the current analysis result and chat.
     */
    fun clearResult() {
        _lastResult.value = null
        _chatMessages.value = emptyList()
    }

    /**
     * Clears the loaded model and resets state.
     */
    fun clearModel() {
        viewModelScope.launch {
            _modelState.value = ModelState.Idle
            _lastResult.value = null
            _chatMessages.value = emptyList()
        }
    }

    fun resetToIdle() {
        _modelState.value = ModelState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Resources are handled by the system or custom logic if needed
    }
}
