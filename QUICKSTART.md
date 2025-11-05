# Quick Start Guide

Get up and running with Categorized Gallery in 5 minutes!

## Prerequisites

- Android Studio (latest version)
- Android device or emulator (Android 7.0+ / API 24+)

## Step-by-Step Setup

### 1. Open Project
```bash
# Clone and open
git clone <repository-url>
cd catagorized_gallery
```

Open Android Studio → File → Open → Select the project folder

### 2. Sync Dependencies

Android Studio will automatically prompt you to sync. If not:
- Click "Sync Project with Gradle Files" (elephant icon in toolbar)
- Wait for sync to complete

### 3. Run the App

**Option A: Physical Device**
1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Click "Run" (green play button) or press Shift+F10
5. Select your device

**Option B: Emulator**
1. Open AVD Manager (phone icon in toolbar)
2. Create new device or select existing
3. Click "Run" or press Shift+F10
4. Wait for emulator to start

### 4. First Launch

1. **Grant Permission**: Tap "Grant Permission" when prompted
2. **Wait for Scan**: App will scan your gallery (few seconds)
3. **Analyze Images**: Tap the floating action button (FAB) at bottom-right
4. **View Results**: Images will be categorized automatically

## Without ML Model (Quick Test)

The app works without downloading the ML model! It uses a fallback classifier based on image properties:
- No setup needed
- Instant results
- Categories based on image dimensions and brightness

## With ML Model (Better Results)

For accurate AI categorization:

### Download Model
```bash
# From project root
cd app/src/main/assets
wget https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224_quant.tgz
tar -xzf mobilenet_v1_1.0_224_quant.tgz
mv mobilenet_v1_1.0_224_quant.tflite mobilenet_v2_1.0_224.tflite
```

Or download manually:
1. Visit [TensorFlow Hub](https://tfhub.dev/tensorflow/lite-model/mobilenet_v2_100_224/1/default/1)
2. Download the `.tflite` file
3. Place in `app/src/main/assets/mobilenet_v2_1.0_224.tflite`

### Rebuild and Run
1. Click "Build → Rebuild Project"
2. Run the app again
3. Now you'll get AI-powered categorization!

## Testing the App

### Add Sample Images (Emulator)

The emulator might not have images. Add some:

1. Open emulator
2. Drag and drop images from your computer
3. Images will appear in the emulator's gallery
4. Open the app and refresh

Or use ADB:
```bash
adb push /path/to/image.jpg /sdcard/Pictures/
```

### Test Categories

Try images with:
- People/faces → "People" category
- Animals (cats, dogs) → "Animals"
- Food/meals → "Food"
- Landscapes → "Nature"
- Buildings → "Architecture"

## Common Issues

### Issue: No images found
**Solution**:
- Check permission granted
- Add images to emulator/device
- Tap refresh button (top right)

### Issue: Gradle sync failed
**Solution**:
- Update Android Studio
- File → Invalidate Caches → Restart
- Check internet connection

### Issue: App crashes on launch
**Solution**:
- Check Android API level (need 24+)
- Check device storage
- See logcat for errors

### Issue: Slow analysis
**Solution**:
- Normal for first time (processes all images)
- Reduce batch size in code
- Use physical device instead of emulator

## What to Try

### 1. Browse Gallery
- Scroll through your categorized photos
- See category badges on images

### 2. Filter by Category
- Tap category icon (top right)
- Select a category
- View only that category

### 3. Refresh Gallery
- Add new photos
- Tap refresh icon
- See new images appear

### 4. Batch Analysis
- Watch progress indicator
- See real-time categorization
- Categories update automatically

## Project Structure Quick Reference

```
app/src/main/
├── assets/               # Put .tflite model here
├── java/.../
│   ├── MainActivity.kt   # Entry point
│   ├── data/
│   │   ├── model/        # Data models
│   │   ├── local/        # Database
│   │   └── repository/   # Data layer
│   ├── ml/               # TensorFlow Lite
│   │   └── ImageClassifier.kt
│   └── ui/
│       ├── screens/      # UI screens
│       └── viewmodel/    # State management
└── res/
    └── values/
        └── strings.xml   # UI strings
```

## Next Steps

1. ✅ App running? Great!
2. 📖 Read [README.md](README.md) for detailed features
3. 🏗️ Check [ARCHITECTURE.md](ARCHITECTURE.md) to understand code
4. 🎨 Customize categories in `ImageClassifier.kt`
5. 🚀 Build your own features!

## Need Help?

- Check [README.md](README.md) troubleshooting section
- See Android Studio logcat for errors
- Open an issue on GitHub

## Development Tips

### Enable Debug Logs
Add to `ImageClassifier.kt`:
```kotlin
private const val DEBUG = true
if (DEBUG) Log.d("Classifier", "Result: $category")
```

### Change Confidence Threshold
Edit `ImageClassifier.kt`:
```kotlin
private const val CONFIDENCE_THRESHOLD = 0.5f // Higher = stricter
```

### Modify Batch Size
Edit `GalleryViewModel.kt`:
```kotlin
fun analyzeImages(batchSize: Int = 10) // Smaller = faster updates
```

---

**Ready to build something awesome? Let's go! 🚀**
