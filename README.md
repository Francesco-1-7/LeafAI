# 🌿 LeafAI

**LeafAI** is an Android application powered by on-device Artificial Intelligence (**LiteRT**), designed to assist botanists and plant enthusiasts in diagnosing plant health through photographic analysis of leaves.

## 🚀 Features
*   **Real-Time Analysis:** Identify plant pathologies directly via your camera.
*   **Privacy First:** All processing happens locally on your device (On-device AI). No data leaves your phone.
*   **AI Botanical Expert:** Provides detailed diagnoses, probable causes, and treatment suggestions.
*   **Interactive Chat:** Ask for deeper insights or specific care instructions in the dedicated chat section.

---

## 🛠️ Installation & Setup

The app is currently in active development. To get it running correctly, please follow these steps:

### 1. Download the App
Download the latest `.apk` file from the [Releases] section and install it on your Android smartphone.

### 2. Configure the AI Model (Required) 🤖
To keep the installation package lightweight, the AI model is not bundled with the app.

1.  **Current Model:** Go to **Hugging Face** and download the base model: [gemma-4-E2B-it.litertlm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it.litertlm).
2.  **Move the File:** Once downloaded, move the model file into the **Download** folder of your smartphone.
3.  **Initialization:** Upon launching LeafAI, use the file picker to select the model from your Download folder to initialize the AI engine.

> [!IMPORTANT]  
> **Upcoming Update:** The model linked above is the current base version. I'm currently **fine-tuning a new custom model** specifically trained on thousands of plant diseases. This specialized version will be released soon to provide even more accurate and professional diagnoses.

---

## 📸 How to Use
1.  **Open LeafAI.**
2.  **Capture:** Take a clear, well-lit photo of a leaf.
3.  **Analyze:** Wait for the AI Botanical Expert to process the image.
4.  **Report:** Review the detailed health report and care recommendations.
5.  **Chat:** Use the chat feature if you need more specific details or have follow-up questions.

---

## 🏗️ Tech Stack
*   **Language:** Kotlin / Java
*   **UI Framework:** Jetpack Compose
*   **AI Engine:** Google LiteRT
*   **Current Model:** Gemma 4 E2B

---
