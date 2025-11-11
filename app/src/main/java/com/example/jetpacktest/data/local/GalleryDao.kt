package com.example.jetpacktest.data.local

import androidx.room.*
import com.example.jetpacktest.data.model.GalleryImage
import com.example.jetpacktest.data.model.ImageCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {

    @Query("SELECT * FROM gallery_images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Query("SELECT * FROM gallery_images WHERE id = :imageId")
    suspend fun getImageById(imageId: Long): GalleryImage?

    @Query("SELECT * FROM gallery_images WHERE category = :category ORDER BY dateAdded DESC")
    fun getImagesByCategory(category: String): Flow<List<GalleryImage>>

    @Query("""
        SELECT category, COUNT(*) as count,
        (SELECT uri FROM gallery_images WHERE category = c.category LIMIT 1) as thumbnailUri
        FROM gallery_images c
        WHERE category IS NOT NULL
        GROUP BY category
        ORDER BY count DESC
    """)
    fun getAllCategories(): Flow<List<ImageCategory>>

    @Query("SELECT * FROM gallery_images WHERE isAnalyzed = 0 LIMIT :limit")
    suspend fun getUnanalyzedImages(limit: Int = 10): List<GalleryImage>

    @Query("SELECT COUNT(*) FROM gallery_images WHERE isAnalyzed = 0")
    suspend fun getUnanalyzedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<GalleryImage>)

    @Update
    suspend fun updateImage(image: GalleryImage)

    @Query("UPDATE gallery_images SET category = :category, confidence = :confidence, isAnalyzed = 1, analyzedAt = :analyzedAt WHERE id = :imageId")
    suspend fun updateImageCategory(imageId: Long, category: String, confidence: Float, analyzedAt: Long)

    @Delete
    suspend fun deleteImage(image: GalleryImage)

    @Query("DELETE FROM gallery_images")
    suspend fun deleteAll()

    @Query("DELETE FROM gallery_images WHERE id NOT IN (:validIds)")
    suspend fun deleteImagesNotIn(validIds: List<Long>)
}
