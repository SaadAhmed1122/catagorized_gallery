package com.gallery.categorized.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gallery.categorized.data.local.GalleryDao
import com.gallery.categorized.data.model.GalleryImage
import com.gallery.categorized.data.model.ImageCategory
import com.gallery.categorized.ml.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val galleryDao: GalleryDao,
    private val mediaStoreRepository: MediaStoreRepository,
    private val imageClassifier: ImageClassifier
) {

    companion object {
        private const val TAG = "GalleryRepository"
    }

    /**
     * Get all images from database
     */
    fun getAllImages(): Flow<List<GalleryImage>> = galleryDao.getAllImages()

    /**
     * Get images by category
     */
    fun getImagesByCategory(category: String): Flow<List<GalleryImage>> =
        galleryDao.getImagesByCategory(category)

    /**
     * Get all categories with image counts
     */
    fun getAllCategories(): Flow<List<ImageCategory>> = galleryDao.getAllCategories()

    /**
     * Sync images from MediaStore to local database
     */
    suspend fun syncImagesFromDevice(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting sync from device...")

            val deviceImages = mediaStoreRepository.loadImagesFromDevice()
            Log.d(TAG, "📥 Loaded ${deviceImages.size} images from MediaStore")

            // Get existing images from database - CRITICAL FIX: Use first() instead of collect()
            val existingImages = galleryDao.getAllImages().first()
            val existingImageIds = existingImages.map { it.id }.toSet()
            Log.d(TAG, "💾 Found ${existingImages.size} images already in database")

            // Insert new images
            val newImages = deviceImages.filter { it.id !in existingImageIds }
            if (newImages.isNotEmpty()) {
                Log.d(TAG, "➕ Inserting ${newImages.size} new images")
                galleryDao.insertImages(newImages)
            } else {
                Log.d(TAG, "✅ No new images to insert")
            }

            // Clean up deleted images
            val currentImageIds = deviceImages.map { it.id }
            if (currentImageIds.isNotEmpty()) {
                galleryDao.deleteImagesNotIn(currentImageIds)
            }

            Log.d(TAG, "✅ Sync complete! New images: ${newImages.size}")
            Result.success(newImages.size)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Analyze unclassified images
     */
    suspend fun analyzeUnclassifiedImages(
        batchSize: Int = 10,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unanalyzedImages = galleryDao.getUnanalyzedImages(batchSize)
            var analyzedCount = 0

            unanalyzedImages.forEachIndexed { index, image ->
                try {
                    val uri = Uri.parse(image.uri)
                    val result = imageClassifier.classifyImage(uri)

                    if (result != null) {
                        galleryDao.updateImageCategory(
                            imageId = image.id,
                            category = result.displayName,
                            confidence = result.confidence,
                            analyzedAt = System.currentTimeMillis()
                        )
                        analyzedCount++
                    } else {
                        // Mark as analyzed even if classification failed
                        galleryDao.updateImageCategory(
                            imageId = image.id,
                            category = "Others",
                            confidence = 0.5f,
                            analyzedAt = System.currentTimeMillis()
                        )
                    }

                    onProgress(index + 1, unanalyzedImages.size)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(analyzedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of unanalyzed images
     */
    suspend fun getUnanalyzedCount(): Int = withContext(Dispatchers.IO) {
        galleryDao.getUnanalyzedCount()
    }

    /**
     * Re-analyze a specific image
     */
    suspend fun reAnalyzeImage(imageId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = galleryDao.getImageById(imageId) ?: return@withContext Result.failure(
                Exception("Image not found")
            )

            val uri = Uri.parse(image.uri)
            val result = imageClassifier.classifyImage(uri)

            if (result != null) {
                galleryDao.updateImageCategory(
                    imageId = imageId,
                    category = result.displayName,
                    confidence = result.confidence,
                    analyzedAt = System.currentTimeMillis()
                )
                Result.success(result.displayName)
            } else {
                Result.failure(Exception("Classification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize the image classifier
     */
    fun initializeClassifier(): Boolean {
        return imageClassifier.initialize()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        imageClassifier.close()
    }
}
