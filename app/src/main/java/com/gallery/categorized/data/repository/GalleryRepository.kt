package com.gallery.categorized.data.repository

import android.content.Context
import android.net.Uri
import com.gallery.categorized.data.local.GalleryDao
import com.gallery.categorized.data.model.GalleryImage
import com.gallery.categorized.data.model.ImageCategory
import com.gallery.categorized.ml.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val galleryDao: GalleryDao,
    private val mediaStoreRepository: MediaStoreRepository,
    private val imageClassifier: ImageClassifier
) {

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
            val deviceImages = mediaStoreRepository.loadImagesFromDevice()

            // Get existing images from database
            val existingImageIds = mutableSetOf<Long>()
            galleryDao.getAllImages().collect { images ->
                existingImageIds.addAll(images.map { it.id })
            }

            // Insert new images
            val newImages = deviceImages.filter { it.id !in existingImageIds }
            if (newImages.isNotEmpty()) {
                galleryDao.insertImages(newImages)
            }

            // Clean up deleted images
            val currentImageIds = deviceImages.map { it.id }
            if (currentImageIds.isNotEmpty()) {
                galleryDao.deleteImagesNotIn(currentImageIds)
            }

            Result.success(newImages.size)
        } catch (e: Exception) {
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
