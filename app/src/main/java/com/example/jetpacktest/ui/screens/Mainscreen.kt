package com.example.jetpacktest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.jetpacktest.ui.viewmodel.GalleryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: GalleryUiState,
    onRefresh: () -> Unit,
    onAnalyze: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("All Images", "Categorized")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Gallery",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        // Unanalyzed count badge
                        if (uiState.unanalyzedCount > 0) {
                            IconButton(onClick = onAnalyze) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(uiState.unanalyzedCount.toString())
                                }
                            }
                        }

                        // Refresh button
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) },
                            icon = {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.PhotoLibrary else Icons.Default.Category,
                                    contentDescription = title
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.unanalyzedCount > 0 && !uiState.isAnalyzing) {
                ExtendedFloatingActionButton(
                    onClick = onAnalyze,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text("Analyze ${uiState.unanalyzedCount} images") }
                )
            }
        }
    ) { padding ->
        when (selectedTabIndex) {
            0 -> AllImagesTab(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            1 -> CategorizedImagesTab(
                uiState = uiState,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}