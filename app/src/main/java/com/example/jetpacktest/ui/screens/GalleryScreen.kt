package com.example.jetpacktest.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.jetpacktest.data.model.GalleryImage
import com.example.jetpacktest.data.model.ImageCategory
import com.example.jetpacktest.ui.viewmodel.GalleryUiState

// Tab 1: All Images Tab
@Composable
fun AllImagesTab(
    uiState: GalleryUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingIndicator()
            }
            uiState.images.isEmpty() -> {
                EmptyState()
            }
            else -> {
                ImageGrid(
                    images = uiState.images,
                    showCategoryBadge = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Analysis progress overlay
        if (uiState.isAnalyzing && uiState.analysisProgress != null) {
            AnalysisProgressOverlay(
                current = uiState.analysisProgress.current,
                total = uiState.analysisProgress.total,
                percentage = uiState.analysisProgress.percentage
            )
        }

        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = {}) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

// Tab 2: Categorized Images Tab
@Composable
fun CategorizedImagesTab(
    uiState: GalleryUiState,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategorySheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingIndicator()
            }
            uiState.categories.isEmpty() -> {
                EmptyState(
                    message = "No categorized images",
                    subtitle = "Analyze images to see them organized by category"
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category selector button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategorySheet = true }
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Category",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = uiState.selectedCategory ?: "All Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Category",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Filtered images
                    val filteredImages = if (uiState.selectedCategory != null) {
                        uiState.images.filter { it.category == uiState.selectedCategory }
                    } else {
                        uiState.images.filter { it.category != null }
                    }

                    if (filteredImages.isEmpty()) {
                        EmptyState(
                            message = "No images in this category",
                            subtitle = "Try selecting a different category"
                        )
                    } else {
                        ImageGrid(
                            images = filteredImages,
                            showCategoryBadge = uiState.selectedCategory == null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Analysis progress overlay
        if (uiState.isAnalyzing && uiState.analysisProgress != null) {
            AnalysisProgressOverlay(
                current = uiState.analysisProgress.current,
                total = uiState.analysisProgress.total,
                percentage = uiState.analysisProgress.percentage
            )
        }

        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = {}) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Category bottom sheet
    if (showCategorySheet) {
        CategoryBottomSheet(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = {
                onCategorySelected(it)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }
}

@Composable
fun ImageGrid(
    images: List<GalleryImage>,
    showCategoryBadge: Boolean,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(images, key = { it.id }) { image ->
            ImageGridItem(
                image = image,
                showCategoryBadge = showCategoryBadge
            )
        }
    }
}

@Composable
fun ImageGridItem(
    image: GalleryImage,
    showCategoryBadge: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = Uri.parse(image.uri),
            contentDescription = image.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Category badge
        if (showCategoryBadge && image.category != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 8.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = image.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBottomSheet(
    categories: List<ImageCategory>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // All categories option
            CategoryItem(
                category = ImageCategory("All Categories", categories.sumOf { it.count }, null),
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Category list
            categories.forEach { category ->
                CategoryItem(
                    category = category,
                    isSelected = selectedCategory == category.category,
                    onClick = { onCategorySelected(category.category) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryItem(
    category: ImageCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.thumbnailUri != null) {
            AsyncImage(
                model = Uri.parse(category.thumbnailUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (category.count > 0) {
                Text(
                    text = "${category.count} images",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading images...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String = "No images found",
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun AnalysisProgressOverlay(
    current: Int,
    total: Int,
    percentage: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analyzing Images",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}