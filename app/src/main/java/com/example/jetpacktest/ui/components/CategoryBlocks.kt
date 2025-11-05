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
    val categories = listOf(
        CategoryInfo(
            name = "Finance",
            icon = Icons.Default.AccountBalance,
            gradientColors = Color(0xFF4CAF50) to Color(0xFF8BC34A)
        ),
        CategoryInfo(
            name = "House",
            icon = Icons.Default.Home,
            gradientColors = Color(0xFFFF9800) to Color(0xFFFFB74D)
        ),
        CategoryInfo(
            name = "Tech",
            icon = Icons.Default.Computer,
            gradientColors = Color(0xFF2196F3) to Color(0xFF64B5F6)
        ),
        CategoryInfo(
            name = "Health",
            icon = Icons.Default.FavoriteBorder,
            gradientColors = Color(0xFFE91E63) to Color(0xFFF48FB1)
        ),
        CategoryInfo(
            name = "Work",
            icon = Icons.Default.Work,
            gradientColors = Color(0xFF9C27B0) to Color(0xFFBA68C8)
        ),
        CategoryInfo(
            name = "Trip",
            icon = Icons.Default.Flight,
            gradientColors = Color(0xFF00BCD4) to Color(0xFF4DD0E1)
        ),
        CategoryInfo(
            name = "Shopping",
            icon = Icons.Default.ShoppingCart,
            gradientColors = Color(0xFFFF5722) to Color(0xFFFF8A65)
        ),
        CategoryInfo(
            name = "Fashion",
            icon = Icons.Default.Style,
            gradientColors = Color(0xFFE91E63) to Color(0xFFF06292)
        ),
        CategoryInfo(
            name = "Politics",
            icon = Icons.Default.Flag,
            gradientColors = Color(0xFF607D8B) to Color(0xFF90A4AE)
        ),
        CategoryInfo(
            name = "Other",
            icon = Icons.Default.Category,
            gradientColors = Color(0xFF9E9E9E) to Color(0xFFBDBDBD)
        )
    )

    fun getCategoryInfo(categoryName: String): CategoryInfo {
        return categories.find { it.name == categoryName }
            ?: CategoryInfo("Other", Icons.Default.Category, Color.Gray to Color.LightGray)
    }
}

@Composable
fun CategoryBlocksGrid(
    categories: List<ImageCategory>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Create a map of category counts
    val categoryCountMap = categories.associate { it.category to it.count }

    // Get all predefined categories with their counts
    val allCategories = CategoryConfig.categories.map { categoryInfo ->
        categoryInfo to (categoryCountMap[categoryInfo.name] ?: 0)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(allCategories) { (categoryInfo, count) ->
            CategoryBlock(
                categoryInfo = categoryInfo,
                count = count,
                onClick = { onCategoryClick(categoryInfo.name) }
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
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = categoryInfo.name,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )

                // Category name and count
                Column {
                    Text(
                        text = categoryInfo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (count > 0) "$count images" else "No images",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Optional: Add count badge
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
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
