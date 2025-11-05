package com.gallery.categorized.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val category: String? = null,
    val confidence: Float? = null,
    val isAnalyzed: Boolean = false,
    val analyzedAt: Long? = null
)

data class ImageCategory(
    val category: String,
    val count: Int,
    val thumbnailUri: String?
)

data class CategoryWithImages(
    val category: String,
    val images: List<GalleryImage>
)
