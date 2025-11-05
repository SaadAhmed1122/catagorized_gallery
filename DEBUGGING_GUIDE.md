# Debugging Guide: "Loading Images..." Stuck Issue

## Problem
The app shows "Loading images..." indefinitely and never displays images.

## Common Causes & Solutions

### 1. Flow Not Collecting Properly

**Issue**: The `observeImages()` function creates a new Flow collection inside a coroutine, which might not properly update the UI state.

**Fix**: Update your `GalleryViewModel.kt`:

```kotlin
package com.example.jetpacktest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetpacktest.data.model.GalleryImage
import com.example.jetpacktest.data.model.ImageCategory
import com.example.jetpacktest.data.repository.GalleryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "GalleryViewModel"

data class GalleryUiState(
    val images: List<GalleryImage> = emptyList(),
    val categories: List<ImageCategory> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val analysisProgress: AnalysisProgress? = null,
    val unanalyzedCount: Int = 0,
    val error: String? = null
)

data class AnalysisProgress(
    val current: Int,
    val total: Int,
    val percentage: Int = if (total > 0) (current * 100 / total) else 0
)

class GalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        initializeClassifier()
        loadData()
    }

    private fun initializeClassifier() {
        viewModelScope.launch {
            try {
                val success = repository.initializeClassifier()
                Log.d(TAG, "Classifier initialized: $success")
                if (!success) {
                    _uiState.update { it.copy(error = "Using fallback classification") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Classifier init error", e)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting loadData()")
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Sync images from device
                Log.d(TAG, "Syncing images from device...")
                val syncResult = repository.syncImagesFromDevice()

                syncResult.fold(
                    onSuccess = { newCount ->
                        Log.d(TAG, "Synced $newCount new images")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Sync failed: ${error.message}", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load images: ${error.message}"
                            )
                        }
                        return@launch
                    }
                )

                // Observe images - CRITICAL: Do this AFTER sync
                Log.d(TAG, "Starting to observe images...")
                repository.getAllImages()
                    .catch { error ->
                        Log.e(TAG, "Images flow error", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error loading images: ${error.message}"
                            )
                        }
                    }
                    .collect { images ->
                        Log.d(TAG, "Received ${images.size} images from repository")
                        _uiState.update {
                            it.copy(
                                images = images,
                                isLoading = false
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "loadData error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }

        // Observe categories separately
        viewModelScope.launch {
            try {
                repository.getAllCategories()
                    .catch { error ->
                        Log.e(TAG, "Categories flow error", error)
                    }
                    .collect { categories ->
                        Log.d(TAG, "Received ${categories.size} categories")
                        _uiState.update { it.copy(categories = categories) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Categories observation error", e)
            }
        }

        // Get unanalyzed count
        viewModelScope.launch {
            try {
                val count = repository.getUnanalyzedCount()
                Log.d(TAG, "Unanalyzed count: $count")
                _uiState.update { it.copy(unanalyzedCount = count) }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting unanalyzed count", e)
            }
        }
    }

    fun refreshImages() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing images...")
                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = repository.syncImagesFromDevice()

                result.fold(
                    onSuccess = { newCount ->
                        Log.d(TAG, "Refresh found $newCount new images")
                        val count = repository.getUnanalyzedCount()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                unanalyzedCount = count,
                                error = if (newCount > 0) "Found $newCount new images" else null
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Refresh failed", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to refresh: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "refreshImages error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun analyzeImages(batchSize: Int = 20) {
        if (_uiState.value.isAnalyzing) {
            Log.d(TAG, "Already analyzing, skipping")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting analysis...")
                _uiState.update { it.copy(isAnalyzing = true, error = null) }

                val result = repository.analyzeUnclassifiedImages(batchSize) { current, total ->
                    Log.d(TAG, "Analysis progress: $current/$total")
                    _uiState.update {
                        it.copy(analysisProgress = AnalysisProgress(current, total))
                    }
                }

                result.fold(
                    onSuccess = { analyzedCount ->
                        Log.d(TAG, "Analysis complete: $analyzedCount images")
                        val count = repository.getUnanalyzedCount()
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                analysisProgress = null,
                                unanalyzedCount = count,
                                error = if (analyzedCount > 0) "Analyzed $analyzedCount images" else null
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Analysis failed", error)
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                analysisProgress = null,
                                error = "Analysis failed: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "analyzeImages error", e)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisProgress = null,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectCategory(category: String?) {
        Log.d(TAG, "Category selected: $category")
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
        repository.cleanup()
    }
}

class GalleryViewModelFactory(
    private val repository: GalleryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### 2. Permission Check Issue

**Issue**: Permission might not be properly granted or checked.

**Fix**: Update your `MainActivity.kt`:

```kotlin
package com.example.jetpacktest

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpacktest.data.local.GalleryDatabase
import com.example.jetpacktest.data.repository.GalleryRepository
import com.example.jetpacktest.data.repository.MediaStoreRepository
import com.example.jetpacktest.ml.ImageClassifier
import com.example.jetpacktest.ui.screens.GalleryScreen
import com.example.jetpacktest.ui.theme.JetpackTestTheme
import com.example.jetpacktest.ui.viewmodel.GalleryViewModel
import com.example.jetpacktest.ui.viewmodel.GalleryViewModelFactory

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Permission result: $isGranted")
        hasStoragePermission = isGranted

        if (!isGranted) {
            Log.w(TAG, "Storage permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate called")

        // Check initial permission
        checkAndRequestPermission()

        setContent {
            JetpackTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasStoragePermission) {
                        Log.d(TAG, "Showing GalleryApp")
                        GalleryApp()
                    } else {
                        Log.d(TAG, "Showing PermissionScreen")
                        PermissionScreen(
                            onRequestPermission = { checkAndRequestPermission() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        Log.d(TAG, "Checking permission: $permission")

        when {
            checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Permission already granted")
                hasStoragePermission = true
            }
            else -> {
                Log.d(TAG, "Requesting permission")
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun GalleryApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Create repository
    val repository = remember {
        Log.d("GalleryApp", "Creating repository")
        val database = GalleryDatabase.getDatabase(context)
        val mediaStoreRepository = MediaStoreRepository(context)
        val classifier = ImageClassifier(context)
        GalleryRepository(
            galleryDao = database.galleryDao(),
            mediaStoreRepository = mediaStoreRepository,
            imageClassifier = classifier
        )
    }

    // Create ViewModel
    val viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()

    Log.d("GalleryApp", "UI State: loading=${uiState.isLoading}, images=${uiState.images.size}, error=${uiState.error}")

    GalleryScreen(
        uiState = uiState,
        onRefresh = { viewModel.refreshImages() },
        onAnalyze = { viewModel.analyzeImages() },
        onCategorySelected = { viewModel.selectCategory(it) }
    )
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app needs access to your photos to organize them by category using AI.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}
```

### 3. Database/Repository Issue

The `syncImagesFromDevice()` might be failing silently. Check logs with:

```bash
adb logcat | grep -E "GalleryViewModel|MainActivity|MediaStore|Repository"
```

### 4. Quick Debug Checklist

1. **Check Logcat**: Look for error messages
   ```bash
   adb logcat | grep -E "GalleryViewModel|ERROR"
   ```

2. **Verify Permission**: Go to device Settings → Apps → Your App → Permissions
   - Should show "Photos and videos" or "Storage" permission granted

3. **Check if device has images**:
   - Open the default Gallery app
   - Ensure there are images to load

4. **Test MediaStore directly**: Add temporary test in MainActivity:
   ```kotlin
   // In onCreate, after permission granted
   lifecycleScope.launch {
       val images = MediaStoreRepository(this@MainActivity).loadImagesFromDevice()
       Log.d(TAG, "Test: Found ${images.size} images")
   }
   ```

5. **Rebuild**: Sometimes Clean Project helps
   - Build → Clean Project
   - Build → Rebuild Project

### 5. Common Fixes

**If images load but UI doesn't update:**
```kotlin
// In your screen, ensure you're collecting state correctly:
val uiState by viewModel.uiState.collectAsState()  // ✅ Correct
// NOT: val uiState = viewModel.uiState.value  // ❌ Wrong
```

**If permission dialog doesn't show:**
```xml
<!-- Verify AndroidManifest.xml has permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**If still stuck, add a manual refresh button:**
```kotlin
Button(onClick = { viewModel.refreshImages() }) {
    Text("Force Refresh")
}
```

## Testing Steps

1. Uninstall the app completely
2. Reinstall
3. Grant permission when prompted
4. Check logcat for debug messages
5. If you see "Received X images from repository" but UI shows loading, the issue is in UI state collection
6. If you don't see "Received X images", the issue is in repository/database

## Still Not Working?

Add this test function to your ViewModel:
```kotlin
fun testLoadImages() {
    viewModelScope.launch {
        _uiState.update { it.copy(
            images = listOf(
                GalleryImage(
                    id = 1L,
                    uri = "content://media/external/images/media/1",
                    displayName = "test.jpg",
                    dateAdded = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis(),
                    size = 1000L,
                    mimeType = "image/jpeg"
                )
            ),
            isLoading = false
        )}
    }
}
```

Call it after init: `testLoadImages()`

If this shows images, the issue is definitely in MediaStore/Repository.
