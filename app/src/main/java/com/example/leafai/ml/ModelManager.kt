package com.example.leafai.ml

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

/**
 * Singleton manager for LiteRT-LM Engine and Conversation to ensure persistence across screens.
 */
object ModelManager {
    private const val TAG = "ModelManager"
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentModelPath: String? = null

    init {
        try {
            System.loadLibrary("litertlm_jni")
            Log.d(TAG, "Native library litertlm_jni loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library litertlm_jni", e)
        }
    }

    fun getEngine(): Engine? = engine
    
    fun getConversation(): Conversation? = conversation

    /**
     * Initializes the engine and conversation if not already loaded.
     */
    fun loadModel(context: android.content.Context, modelPath: String) {
        if (engine != null && currentModelPath == modelPath) {
            Log.d(TAG, "Model already loaded, skipping re-initialization")
            return
        }

        try {
            close() // Close previous resources

            Log.d(TAG, "Initializing LiteRT-LM Engine (Senior Config - CPU/Stability focus)...")

            // 1. Setup persistent cache for KV fragments
            val cacheDir = java.io.File(context.cacheDir, "litert_cache").apply {
                if (!exists()) mkdirs() 
            }

            // 2. Define Backend - strictly CPU for uniform behavior across Android tiers
            val cpuBackend = Backend.CPU(4)

            // 3. EngineConfig: Crucial parameters for DYNAMIC_UPDATE_SLICE stability
            // maxNumTokens: Defines the KV cache dimension. Must accommodate prompt + image embeddings + completion.
            // 2048 is recommended for multimodal models to avoid invocation failures.
            val config = EngineConfig(
                modelPath = modelPath,
                backend = cpuBackend,
                visionBackend = cpuBackend,
                audioBackend = cpuBackend,
                maxNumTokens = 2048,
                maxNumImages = 1,
                cacheDir = cacheDir.absolutePath
            )

            val newEngine = Engine(config).apply {
                initialize()
            }
            engine = newEngine
            
            // 4. ConversationConfig with Sampler tuning for reliability
            // We use standard greedy sampling (temp ~0) for diagnostic accuracy.
            val samplerConfig = SamplerConfig(40, 0.95, 0.0, 42)
            val convConfig = ConversationConfig(
                systemInstruction = null,
                initialMessages = emptyList(),
                tools = emptyList(),
                samplerConfig = samplerConfig
            )
            conversation = newEngine.createConversation(convConfig)
            
            currentModelPath = modelPath
            Log.d(TAG, "Engine/Conversation initialized. Context size: 2048 tokens. Cache: ${cacheDir.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Senior-level init failed: model architecture might be incompatible with LiteRT-LM runtime version.", e)
            throw e
        }
    }

    /**
     * Releases resources. Should be called when the app is closing.
     */
    fun close() {
        try {
            conversation?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing resources", e)
        } finally {
            conversation = null
            engine = null
            currentModelPath = null
        }
    }
}
