package com.example.jetpacktest.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetpacktest.data.model.GalleryImage
import com.example.jetpacktest.data.model.ImageCategory
import com.example.jetpacktest.data.repository.GalleryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        initializeClassifier()
        loadData()
        observeImages()
        observeCategories()
    }

    /**
     * Initialize the ML classifier
     */
    private fun initializeClassifier() {
        viewModelScope.launch {
            val success = repository.initializeClassifier()
            if (!success) {
                _uiState.update { it.copy(error = "ML model not available. Using fallback classification.") }
            }
        }
    }

    /**
     * Load initial data
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Sync images from device
            repository.syncImagesFromDevice()

            // Get unanalyzed count
            val count = repository.getUnanalyzedCount()
            _uiState.update { it.copy(unanalyzedCount = count, isLoading = false) }
        }
    }

    /**
     * Observe images from repository
     */
    private fun observeImages() {
        viewModelScope.launch {
            val selectedCategory = _uiState.value.selectedCategory

            val imagesFlow = if (selectedCategory != null) {
                repository.getImagesByCategory(selectedCategory)
            } else {
                repository.getAllImages()
            }

            imagesFlow.collect { images ->
                _uiState.update { it.copy(images = images) }
            }
        }
    }

    /**
     * Observe categories from repository
     */
    private fun observeCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    /**
     * Refresh images from device
     */
    fun refreshImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.syncImagesFromDevice()

            result.fold(
                onSuccess = { newCount ->
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to sync images: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Analyze unclassified images
     */
    fun analyzeImages(batchSize: Int = 20) {
        if (_uiState.value.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }

            val result = repository.analyzeUnclassifiedImages(batchSize) { current, total ->
                _uiState.update {
                    it.copy(analysisProgress = AnalysisProgress(current, total))
                }
            }

            result.fold(
                onSuccess = { analyzedCount ->
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
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysisProgress = null,
                            error = "Analysis failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Select a category to filter images
     */
    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        observeImages() // Re-observe with new filter
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * Factory for creating GalleryViewModel
 */
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
