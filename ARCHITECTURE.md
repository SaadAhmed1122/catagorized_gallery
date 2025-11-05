# Architecture Documentation

## Overview

This document describes the architecture and design decisions for the Categorized Gallery Android app.

## Architecture Pattern

The app follows **Clean Architecture** principles combined with **MVVM (Model-View-ViewModel)** pattern:

### Layers

#### 1. Presentation Layer (UI)
- **Technology**: Jetpack Compose
- **Components**:
  - `MainActivity.kt`: Entry point, handles permissions
  - `GalleryScreen.kt`: Main UI composables
  - `Theme.kt`: Material Design 3 theme
- **Responsibilities**:
  - Render UI based on state
  - Handle user interactions
  - Display data from ViewModel

#### 2. ViewModel Layer
- **Technology**: Android Architecture Components ViewModel
- **Components**:
  - `GalleryViewModel.kt`: Manages UI state and business logic
  - `GalleryUiState`: Immutable state container
- **Responsibilities**:
  - Manage UI state
  - Handle business logic
  - Coordinate between repository and UI
  - Survive configuration changes

#### 3. Domain Layer
- **Components**:
  - Data models (`GalleryImage`, `ImageCategory`)
  - Use cases (implicit in ViewModel)
- **Responsibilities**:
  - Define business entities
  - Contain business rules

#### 4. Data Layer
- **Components**:
  - `GalleryRepository`: Main data coordinator
  - `MediaStoreRepository`: Device gallery access
  - `GalleryDao`: Database operations
  - `ImageClassifier`: ML inference
- **Responsibilities**:
  - Manage data sources
  - Coordinate between local DB, MediaStore, and ML
  - Provide clean API to upper layers

## Data Flow

```
User Action → Compose UI → ViewModel → Repository → Data Sources
                ↑                                         ↓
                └─────── State Update ←─────────────────┘
```

### Example Flow: Analyzing Images

1. User taps "Analyze" button
2. UI calls `viewModel.analyzeImages()`
3. ViewModel calls `repository.analyzeUnclassifiedImages()`
4. Repository:
   - Gets unanalyzed images from Room DB
   - For each image:
     - Loads via `MediaStoreRepository`
     - Classifies via `ImageClassifier`
     - Updates result in Room DB
5. Room DB emits new data via Flow
6. ViewModel updates `UiState`
7. Compose UI recomposes with new state

## Key Design Decisions

### 1. Why MVVM?

- **Separation of Concerns**: Clear boundaries between UI, logic, and data
- **Testability**: ViewModels can be unit tested
- **Lifecycle Aware**: Survives configuration changes
- **Reactive**: Uses Kotlin Flow for reactive updates

### 2. Why Jetpack Compose?

- **Modern**: Latest Android UI toolkit
- **Declarative**: UI as a function of state
- **Less Boilerplate**: Compared to XML layouts
- **Type Safe**: Kotlin-first design
- **Performance**: Efficient recomposition

### 3. Why Room Database?

- **Offline First**: Works without network
- **Type Safe**: Compile-time query verification
- **Reactive**: Flow-based updates
- **Migration Support**: Easy schema updates
- **Performance**: Fast local queries

### 4. Repository Pattern

```kotlin
interface DataSource {
    // Abstract data operations
}

class Repository(
    private val localDb: LocalDataSource,
    private val remote: RemoteDataSource,
    private val ml: MLDataSource
) {
    // Coordinate multiple data sources
    // Provide single source of truth
}
```

**Benefits**:
- Single source of truth
- Testability through abstraction
- Easy to add new data sources
- Centralized caching logic

### 5. State Management

Using `StateFlow` for reactive state:

```kotlin
private val _uiState = MutableStateFlow(GalleryUiState())
val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
```

**Benefits**:
- Immutable state
- Reactive updates
- Lifecycle aware
- No memory leaks

## Module Organization

```
app/
├── data/           # Data layer
│   ├── local/      # Room database
│   ├── model/      # Data models
│   └── repository/ # Repository implementations
├── ml/             # Machine learning
├── ui/             # UI layer
│   ├── screens/    # Screen composables
│   ├── theme/      # App theme
│   └── viewmodel/  # ViewModels
└── util/           # Utilities (if needed)
```

## Dependency Graph

```
MainActivity
    ↓
GalleryScreen
    ↓
GalleryViewModel
    ↓
GalleryRepository
    ↓ ↓ ↓
    ↓ ↓ └→ ImageClassifier (TensorFlow Lite)
    ↓ └──→ MediaStoreRepository (MediaStore API)
    └────→ GalleryDao (Room DB)
```

## Concurrency Strategy

### Coroutines Scope Hierarchy

1. **ViewModelScope**: For ViewModel operations
   - Automatically cancelled when ViewModel is cleared
   - Used for all user-initiated operations

2. **IO Dispatcher**: For database and file operations
   ```kotlin
   withContext(Dispatchers.IO) {
       // Heavy operations
   }
   ```

3. **Flow**: For reactive data streams
   ```kotlin
   galleryDao.getAllImages()
       .flowOn(Dispatchers.IO)
       .collect { images -> }
   ```

## Error Handling Strategy

### Result Pattern

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
```

### Error Propagation

1. **Data Layer**: Return Result types
2. **ViewModel**: Handle errors, update UI state
3. **UI**: Display errors to user

## Testing Strategy

### Unit Tests
- ViewModels: Test business logic
- Repository: Test data coordination
- Classifier: Test ML inference

### Integration Tests
- Database operations
- Repository integration

### UI Tests
- Compose UI testing
- User flow testing

## Performance Optimizations

### 1. Image Loading
- **Coil**: Efficient image loading with caching
- **Size Optimization**: Load only required size
- **Memory Cache**: Prevent repeated loading

### 2. Database
- **Indexing**: Index frequently queried columns
- **Batch Operations**: Insert multiple images at once
- **Flow**: Reactive updates without polling

### 3. ML Inference
- **Batch Processing**: Analyze multiple images
- **GPU Acceleration**: Use GPU for faster inference
- **Async Processing**: Non-blocking operations

### 4. UI
- **LazyGrid**: Efficient list rendering
- **State Hoisting**: Minimize recomposition
- **Remember**: Cache expensive computations

## Security Considerations

### Permissions
- Request only necessary permissions
- Handle permission denial gracefully
- Use scoped storage (Android 10+)

### Data Privacy
- All data stored locally
- No network transmission
- User controls all data

## Scalability

### Adding New Features

1. **New Category**: Update `CATEGORY_MAPPINGS`
2. **New Screen**: Create composable in `ui/screens/`
3. **New Data Source**: Add to `Repository`
4. **New ML Model**: Replace in `assets/`, update `ImageClassifier`

### Migration Path

- Room supports migrations for schema changes
- Proguard rules protect important classes
- Backward compatible API design

## Future Architecture Improvements

### 1. Dependency Injection
- Add **Hilt** or **Koin**
- Improve testability
- Reduce boilerplate

### 2. Multi-Module
- Separate feature modules
- Parallel builds
- Better encapsulation

### 3. Use Cases Layer
- Explicit use case classes
- Better separation
- More testable

### 4. Paging 3
- Paginated image loading
- Better memory management
- Smoother scrolling

### 5. WorkManager
- Background analysis
- Periodic sync
- Battery efficient

## References

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [TensorFlow Lite Android](https://www.tensorflow.org/lite/guide/android)
