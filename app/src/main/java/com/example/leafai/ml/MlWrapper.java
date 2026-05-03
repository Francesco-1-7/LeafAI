package com.example.leafai.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.Message;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Interface for leaf analysis using the singleton ModelManager.
 */
public class MlWrapper {
    private static final String TAG = "MlWrapper";

    public MlWrapper(Context context) {
    }

    /**
     * Delegates model loading to the singleton manager.
     */
    public void loadModel(Context context, String modelPath) {
        ModelManager.INSTANCE.loadModel(context, modelPath);
    }

    /**
     * Analyzes an image using the persistent singleton conversation.
     * Engineered for stability: Handles bitmap scaling and multimodal content construction.
     */
    public String analyzeLeaf(Bitmap bitmap, String plantName) {
        Conversation conversation = ModelManager.INSTANCE.getConversation();
        if (conversation == null) return "Modello non caricato";

        try {
            Log.d(TAG, "Senior Analysis: Processing leaf bitmap...");

            // 1. Scale down bitmap to 448x448.
            // Some models (like PaliGemma or newer Gemma-Vision) benefit from higher resolution.
            // 448x448 is a good balance between detail and memory usage.
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 448, 448, true);
            
            // 2. Convert to JPEG byte array with higher quality
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            byte[] byteArray = stream.toByteArray();
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle();
            }

            // 3. Construct multimodal contents
            // Refined prompt to force specialized identification and avoid generic "healthy" hallucinations.
            Content imageContent = new Content.ImageBytes(byteArray);
            
            String promptText = "You're an expert botanist. Analyze this leaf closely.\n";
            if (plantName != null && !plantName.trim().isEmpty()) {
                promptText += "The user identified this plant as: " + plantName + ".\n";
            }
            promptText += "Analyze the leaf and provide an expert response structured as follows:\n" +
                          "1. Detailed health diagnosis based on visible spots, wilting, or discoloration.\n" +
                          "2. Possible causes of the detected issues.\n" +
                          "3. Recommended treatment or next steps.\n" +
                          "Strictly avoid any preamble about plant identification or uncertainty.";
            
            Content textContent = new Content.Text(promptText);
            
            Contents contents = Contents.Companion.of(imageContent, textContent);

            // 4. Send message - Blocking call on background thread (handled by ViewModel)
            Log.d(TAG, "Senior Analysis: Invoking nativeSendMessage with " + byteArray.length + " bytes.");
            Message response = conversation.sendMessage(contents, new HashMap<>());
            
            // Temporary simple parsing for build stability
            // We will inspect the exact structure in the log
            String responseText = response.toString();
            
            Log.d(TAG, "Raw response received: " + responseText);

            if (responseText.isEmpty() || responseText.contains("items=[]")) {
                return "Il modello non ha restituito una risposta valida. " +
                       "Assicurati di usare un modello Gemma-Vision (.tflite) e che l'immagine sia chiara.";
            }
            
            return responseText;
        } catch (Exception e) {
            Log.e(TAG, "Internal Engine Error during invocation", e);
            
            if (e.getMessage() != null && e.getMessage().contains("DYNAMIC_UPDATE_SLICE")) {
                return "Errore Critico: Il modello ha esaurito lo spazio di memoria interna (KV Cache). " +
                       "Riavvia l'applicazione o prova con un'immagine meno complessa.";
            }

            return "Errore nell'analisi: " + e.getMessage();
        }
    }

    public String generateResponse(String prompt) {
        Conversation conversation = ModelManager.INSTANCE.getConversation();
        if (conversation == null) return "Modello non caricato";
        try {
            Log.d(TAG, "Chat Follow-up: Sending text-only prompt: " + prompt);
            
            // For follow-up chat, we should ensure the prompt is clear that it's a continuation
            // though the conversation object should maintain state if the SDK supports it.
            // If the SDK expects images in EVERY turn for multimodal conversations, this might fail,
            // but usually, once an image is in context, subsequent turns can be text-only.
            
            Message response = conversation.sendMessage(prompt, new HashMap<>());
            String responseText = response.toString();
            
            Log.d(TAG, "Chat response received: " + responseText);
            return responseText;
        } catch (Exception e) {
            Log.e(TAG, "Error in generateResponse", e);
            return "Errore nella chat: " + e.getMessage();
        }
    }
}
