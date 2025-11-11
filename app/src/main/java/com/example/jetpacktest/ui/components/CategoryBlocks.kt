package com.example.jetpacktest.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpacktest.data.model.ImageCategory

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val gradientColors: Pair<Color, Color>
)

object CategoryConfig {
    // Predefined color palette for dynamic categories
    private val colorPalette = listOf(
        Color(0xFF2196F3) to Color(0xFF64B5F6), // Blue
        Color(0xFF4CAF50) to Color(0xFF8BC34A), // Green
        Color(0xFFFF9800) to Color(0xFFFFB74D), // Orange
        Color(0xFFE91E63) to Color(0xFFF48FB1), // Pink
        Color(0xFF9C27B0) to Color(0xFFBA68C8), // Purple
        Color(0xFF00BCD4) to Color(0xFF4DD0E1), // Cyan
        Color(0xFFFF5722) to Color(0xFFFF8A65), // Deep Orange
        Color(0xFF3F51B5) to Color(0xFF7986CB), // Indigo
        Color(0xFF009688) to Color(0xFF4DB6AC), // Teal
        Color(0xFFFF6F00) to Color(0xFFFFA726), // Amber
        Color(0xFF673AB7) to Color(0xFF9575CD), // Deep Purple
        Color(0xFFCDDC39) to Color(0xFFDCE775), // Lime
    )

    // Generate consistent CategoryInfo based on category name
    fun getCategoryInfo(categoryName: String): CategoryInfo {
        // Use hash to get consistent color for same category name
        val colorIndex = kotlin.math.abs(categoryName.hashCode()) % colorPalette.size
        val colors = colorPalette[colorIndex]

        // Use a generic icon for all categories
        val icon = Icons.Default.Category

        // Capitalize and format the category name
        val formattedName = categoryName
            .split(" ", "_", "-")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

        return CategoryInfo(
            name = formattedName,
            icon = icon,
            gradientColors = colors
        )
    }
}

@Composable
fun CategoryBlocksGrid(
    categories: List<ImageCategory>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sort categories by count (descending) and then by name
    val sortedCategories = categories
        .sortedWith(compareByDescending<ImageCategory> { it.count }.thenBy { it.category })

    LazyVerticalGrid(
        columns = GridCells.Fixed(1), // Single column for horizontal cards
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(sortedCategories) { imageCategory ->
            val categoryInfo = CategoryConfig.getCategoryInfo(imageCategory.category)
            CategoryBlock(
                categoryInfo = categoryInfo,
                count = imageCategory.count,
                onClick = { onCategoryClick(imageCategory.category) }
            )
        }
    }
}

@Composable
fun CategoryBlock(
    categoryInfo: CategoryInfo,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp), // Fixed smaller height
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            categoryInfo.gradientColors.first,
                            categoryInfo.gradientColors.second
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = categoryInfo.name,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )

                // Category name and count
                Column {
                    Text(
                        text = categoryInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (count > 0) "$count images" else "No images",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Count badge
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
