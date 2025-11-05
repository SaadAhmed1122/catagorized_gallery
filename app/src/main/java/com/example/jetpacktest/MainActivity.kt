package com.example.jetpacktest

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.example.jetpacktest.ui.theme.CategorizedGalleryTheme
import com.example.jetpacktest.ui.screens.MainScreen
import com.example.jetpacktest.ui.viewmodel.GalleryViewModel
import com.example.jetpacktest.ui.viewmodel.GalleryViewModelFactory

class MainActivity : ComponentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial permission
        checkAndRequestPermission()

        setContent {
            CategorizedGalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasStoragePermission) {
                        GalleryApp()
                    } else {
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

        when {
            checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                hasStoragePermission = true
            }
            else -> {
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

    MainScreen(
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