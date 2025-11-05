package com.example.jetpacktest.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier as TFLiteImageClassifier
import java.io.IOException

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val displayName: String
)

class ImageClassifier(private val context: Context) {

    private var classifier: TFLiteImageClassifier? = null
    private var isInitialized = false

    companion object {
        private const val MODEL_FILE = "mobilenet_v2_1.0_224.tflite"
        private const val MAX_RESULTS = 3
        private const val CONFIDENCE_THRESHOLD = 0.3f
        private const val IMAGE_SIZE = 224

        // Simplified category mapping for common objects
        private val CATEGORY_MAPPINGS = mapOf(
            "person" to "People",
            "face" to "People",
            "cat" to "Animals",
            "dog" to "Animals",
            "bird" to "Animals",
            "horse" to "Animals",
            "sheep" to "Animals",
            "cow" to "Animals",
            "elephant" to "Animals",
            "bear" to "Animals",
            "zebra" to "Animals",
            "giraffe" to "Animals",
            "animal" to "Animals",
            "food" to "Food",
            "pizza" to "Food",
            "cake" to "Food",
            "bread" to "Food",
            "fruit" to "Food",
            "vegetable" to "Food",
            "banana" to "Food",
            "apple" to "Food",
            "sandwich" to "Food",
            "hot dog" to "Food",
            "car" to "Vehicles",
            "truck" to "Vehicles",
            "bus" to "Vehicles",
            "train" to "Vehicles",
            "motorcycle" to "Vehicles",
            "bicycle" to "Vehicles",
            "airplane" to "Vehicles",
            "boat" to "Vehicles",
            "building" to "Architecture",
            "house" to "Architecture",
            "bridge" to "Architecture",
            "sky" to "Nature",
            "mountain" to "Nature",
            "beach" to "Nature",
            "ocean" to "Nature",
            "tree" to "Nature",
            "flower" to "Nature",
            "plant" to "Nature",
            "forest" to "Nature",
            "sunset" to "Nature",
            "computer" to "Technology",
            "phone" to "Technology",
            "laptop" to "Technology",
            "keyboard" to "Technology",
            "screen" to "Technology",
            "book" to "Objects",
            "furniture" to "Objects",
            "chair" to "Objects",
            "table" to "Objects",
            "bottle" to "Objects",
            "cup" to "Objects",
            "glass" to "Objects"
        )
    }

    /**
     * Initialize the TensorFlow Lite classifier
     * Note: The model file should be placed in app/src/main/assets/
     */
    fun initialize(): Boolean {
        return try {
            // Try to create classifier with the model
            val options = TFLiteImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(MAX_RESULTS)
                .setScoreThreshold(CONFIDENCE_THRESHOLD)
                .build()

            classifier = TFLiteImageClassifier.createFromFileAndOptions(
                context,
                MODEL_FILE,
                options
            )
            isInitialized = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            // If model file is not available, we'll return mock results
            isInitialized = false
            false
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
            false
        }
    }

    /**
     * Classify an image from URI
     */
    suspend fun classifyImage(imageUri: Uri): ClassificationResult? {
        return try {
            val bitmap = loadBitmapFromUri(imageUri) ?: return null
            classifyBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Classify a bitmap image
     */
    private fun classifyBitmap(bitmap: Bitmap): ClassificationResult? {
        if (!isInitialized || classifier == null) {
            // Return mock classification for demo purposes
            return generateMockClassification(bitmap)
        }

        return try {
            // Resize bitmap to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                IMAGE_SIZE,
                IMAGE_SIZE,
                true
            )

            // Create TensorImage from bitmap
            val tensorImage = TensorImage.fromBitmap(resizedBitmap)

            // Run inference
            val results: List<Classifications> = classifier!!.classify(tensorImage)

            // Process results
            if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                val topCategory = results[0].categories[0]
                val label = topCategory.label.lowercase()
                val confidence = topCategory.score

                // Map to simplified category
                val category = mapToCategory(label)

                ClassificationResult(
                    label = label,
                    confidence = confidence,
                    displayName = category
                )
            } else {
                ClassificationResult("unknown", 0.5f, "Others")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateMockClassification(bitmap)
        }
    }

    /**
     * Map TensorFlow label to simplified category
     */
    private fun mapToCategory(label: String): String {
        // Check direct mapping
        CATEGORY_MAPPINGS[label]?.let { return it }

        // Check if label contains any mapped keyword
        for ((keyword, category) in CATEGORY_MAPPINGS) {
            if (label.contains(keyword, ignoreCase = true)) {
                return category
            }
        }

        // Default category
        return "Others"
    }

    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate mock classification for demo/testing purposes
     * This is used when the model file is not available
     */
    private fun generateMockClassification(bitmap: Bitmap): ClassificationResult {
        // Simple heuristic based on image properties
        val avgBrightness = calculateAverageBrightness(bitmap)
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val category = when {
            aspectRatio > 1.5f -> "Nature" // Wide images -> landscapes
            aspectRatio < 0.7f -> "People" // Tall images -> portraits
            avgBrightness > 180 -> "Food" // Bright images
            avgBrightness < 80 -> "Architecture" // Dark images
            else -> "Others"
        }

        return ClassificationResult(
            label = category.lowercase(),
            confidence = 0.65f,
            displayName = category
        )
    }

    /**
     * Calculate average brightness of bitmap
     */
    private fun calculateAverageBrightness(bitmap: Bitmap): Int {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        var totalBrightness = 0L
        val pixels = IntArray(50 * 50)
        scaledBitmap.getPixels(pixels, 0, 50, 0, 0, 50, 50)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            totalBrightness += (r + g + b) / 3
        }

        return (totalBrightness / pixels.size).toInt()
    }

    /**
     * Clean up resources
     */
    fun close() {
        classifier?.close()
        classifier = null
        isInitialized = false
    }
}
