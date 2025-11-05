# QUICK FIX: Loading Images Stuck

## TL;DR - Most Common Fix

The issue is usually that **Flow collection happens in the wrong place**. Here's the fix:

### Replace your `loadData()` function in GalleryViewModel:

```kotlin
private fun loadData() {
    // Start observing images FIRST
    viewModelScope.launch {
        repository.getAllImages()
            .catch { error ->
                Log.e(TAG, "Images flow error", error)
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
            .collect { images ->
                Log.d(TAG, "Received ${images.size} images")
                _uiState.update { it.copy(images = images, isLoading = false) }
            }
    }

    // Observe categories
    viewModelScope.launch {
        repository.getAllCategories()
            .collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
    }

    // Then sync in background
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repository.syncImagesFromDevice()
        val count = repository.getUnanalyzedCount()
        _uiState.update { it.copy(unanalyzedCount = count) }
    }
}
```

## Why This Fixes It

1. **Room Flow emits immediately**: Even if DB is empty, it emits `emptyList()`
2. **This sets `isLoading = false`**: Removes the loading indicator
3. **Sync happens in background**: New images appear automatically via Flow

## Test It

Add logging to see what's happening:

```kotlin
private const val TAG = "GalleryViewModel"

// In init block:
init {
    Log.d(TAG, "ViewModel created")
    loadData()
}

// In collect:
.collect { images ->
    Log.d(TAG, "GOT ${images.size} IMAGES") // <- Should see this!
    _uiState.update { it.copy(images = images, isLoading = false) }
}
```

Then run:
```bash
adb logcat | grep GalleryViewModel
```

You should see:
```
GalleryViewModel: ViewModel created
GalleryViewModel: GOT 0 IMAGES  # or GOT X IMAGES
```

If you see "GOT 0 IMAGES" and your device has photos:
- Permission not granted
- MediaStore query failing
- Database not syncing

If you don't see "GOT X IMAGES" at all:
- Flow not collecting
- Crash in ViewModel init
- Repository not created

## Nuclear Option: Force Images

If nothing works, temporarily add mock data to verify UI works:

```kotlin
init {
    loadData()

    // TEMPORARY TEST
    viewModelScope.launch {
        delay(1000) // Wait 1 second
        _uiState.update { it.copy(
            isLoading = false,
            images = listOf(
                GalleryImage(
                    id = 1,
                    uri = "android.resource://com.example.jetpacktest/drawable/ic_launcher",
                    displayName = "Test Image",
                    dateAdded = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis(),
                    size = 1000,
                    mimeType = "image/png"
                )
            )
        )}
    }
}
```

If this shows UI with image, then your problem is 100% in data loading, not UI.

## Full Debugging Guide

See [DEBUGGING_GUIDE.md](DEBUGGING_GUIDE.md) for comprehensive troubleshooting.
