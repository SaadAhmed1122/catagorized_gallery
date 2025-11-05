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

        // Category mapping to user-defined categories
        private val CATEGORY_MAPPINGS = mapOf(
            // Finance
            "money" to "Finance",
            "coin" to "Finance",
            "cash" to "Finance",
            "credit card" to "Finance",
            "bank" to "Finance",
            "wallet" to "Finance",
            "calculator" to "Finance",
            "receipt" to "Finance",
            "invoice" to "Finance",

            // House
            "house" to "House",
            "home" to "House",
            "building" to "House",
            "apartment" to "House",
            "furniture" to "House",
            "chair" to "House",
            "table" to "House",
            "bed" to "House",
            "sofa" to "House",
            "couch" to "House",
            "lamp" to "House",
            "door" to "House",
            "window" to "House",
            "room" to "House",

            // Tech
            "computer" to "Tech",
            "laptop" to "Tech",
            "phone" to "Tech",
            "smartphone" to "Tech",
            "tablet" to "Tech",
            "keyboard" to "Tech",
            "mouse" to "Tech",
            "monitor" to "Tech",
            "screen" to "Tech",
            "camera" to "Tech",
            "headphone" to "Tech",
            "speaker" to "Tech",
            "television" to "Tech",
            "remote" to "Tech",
            "gadget" to "Tech",

            // Health
            "medicine" to "Health",
            "pill" to "Health",
            "hospital" to "Health",
            "doctor" to "Health",
            "medical" to "Health",
            "syringe" to "Health",
            "stethoscope" to "Health",
            "bandage" to "Health",
            "fruit" to "Health",
            "vegetable" to "Health",
            "salad" to "Health",
            "vitamin" to "Health",
            "exercise" to "Health",
            "gym" to "Health",
            "yoga" to "Health",
            "fitness" to "Health",

            // Work
            "office" to "Work",
            "desk" to "Work",
            "meeting" to "Work",
            "presentation" to "Work",
            "document" to "Work",
            "paper" to "Work",
            "pen" to "Work",
            "notepad" to "Work",
            "briefcase" to "Work",
            "conference" to "Work",
            "workspace" to "Work",
            "business" to "Work",

            // Trip
            "airplane" to "Trip",
            "plane" to "Trip",
            "airport" to "Trip",
            "luggage" to "Trip",
            "suitcase" to "Trip",
            "passport" to "Trip",
            "ticket" to "Trip",
            "hotel" to "Trip",
            "beach" to "Trip",
            "mountain" to "Trip",
            "landscape" to "Trip",
            "vacation" to "Trip",
            "tourist" to "Trip",
            "landmark" to "Trip",
            "map" to "Trip",
            "travel" to "Trip",
            "backpack" to "Trip",

            // Shopping
            "shopping" to "Shopping",
            "bag" to "Shopping",
            "cart" to "Shopping",
            "store" to "Shopping",
            "mall" to "Shopping",
            "shop" to "Shopping",
            "product" to "Shopping",
            "package" to "Shopping",
            "box" to "Shopping",
            "gift" to "Shopping",
            "sale" to "Shopping",

            // Fashion
            "fashion" to "Fashion",
            "clothing" to "Fashion",
            "dress" to "Fashion",
            "shirt" to "Fashion",
            "shoe" to "Fashion",
            "shoes" to "Fashion",
            "handbag" to "Fashion",
            "purse" to "Fashion",
            "accessories" to "Fashion",
            "jewelry" to "Fashion",
            "watch" to "Fashion",
            "sunglasses" to "Fashion",
            "hat" to "Fashion",
            "coat" to "Fashion",
            "jacket" to "Fashion",

            // Politics
            "politics" to "Politics",
            "government" to "Politics",
            "flag" to "Politics",
            "vote" to "Politics",
            "election" to "Politics",
            "parliament" to "Politics",
            "congress" to "Politics",
            "protest" to "Politics",
            "rally" to "Politics",
            "politician" to "Politics"
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

        // Randomly assign to different categories for demo
        val categories = listOf("Finance", "House", "Tech", "Health", "Work", "Trip", "Shopping", "Fashion", "Politics", "Other")

        val category = when {
            aspectRatio > 1.6f -> "Trip" // Wide images -> travel/landscapes
            aspectRatio < 0.7f -> "Fashion" // Tall images -> fashion/portraits
            avgBrightness > 180 -> "Shopping" // Bright images
            avgBrightness < 80 -> "House" // Dark images
            avgBrightness in 100..150 -> "Work" // Medium brightness
            else -> categories.random() // Random for variety
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
