#!/bin/bash
# Script to download better ML models for image classification

echo "🎯 Image Classification Model Downloader"
echo "=========================================="
echo ""
echo "Select which model to download:"
echo ""
echo "1) EfficientNet-Lite0 (4.3MB) - Fast, Good accuracy"
echo "2) EfficientNet-Lite1 (5.4MB) - Balanced (RECOMMENDED)"
echo "3) EfficientNet-Lite2 (6.9MB) - Slower, Best accuracy"
echo "4) MobileNetV3-Large (3.0MB) - Fast upgrade from V2"
echo "5) MobileNetV3-Small (2.5MB) - Very fast"
echo ""
read -p "Enter choice (1-5): " choice

# Create assets directory if it doesn't exist
mkdir -p app/src/main/assets/

case $choice in
    1)
        echo "📥 Downloading EfficientNet-Lite0..."
        curl -L "https://tfhub.dev/tensorflow/lite-model/efficientnet/lite0/uint8/2?lite-format=tflite" \
            -o app/src/main/assets/efficientnet_lite0.tflite
        MODEL_NAME="efficientnet_lite0.tflite"
        ;;
    2)
        echo "📥 Downloading EfficientNet-Lite1 (RECOMMENDED)..."
        curl -L "https://tfhub.dev/tensorflow/lite-model/efficientnet/lite1/uint8/2?lite-format=tflite" \
            -o app/src/main/assets/efficientnet_lite1.tflite
        MODEL_NAME="efficientnet_lite1.tflite"
        ;;
    3)
        echo "📥 Downloading EfficientNet-Lite2..."
        curl -L "https://tfhub.dev/tensorflow/lite-model/efficientnet/lite2/uint8/2?lite-format=tflite" \
            -o app/src/main/assets/efficientnet_lite2.tflite
        MODEL_NAME="efficientnet_lite2.tflite"
        ;;
    4)
        echo "📥 Downloading MobileNetV3-Large..."
        curl -L "https://storage.googleapis.com/mobilenet_v3/checkpoints/v3-large_224_1.0_uint8.tflite" \
            -o app/src/main/assets/mobilenet_v3_large.tflite
        MODEL_NAME="mobilenet_v3_large.tflite"
        ;;
    5)
        echo "📥 Downloading MobileNetV3-Small..."
        curl -L "https://storage.googleapis.com/mobilenet_v3/checkpoints/v3-small_224_1.0_uint8.tflite" \
            -o app/src/main/assets/mobilenet_v3_small.tflite
        MODEL_NAME="mobilenet_v3_small.tflite"
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Model downloaded successfully!"
    echo ""
    echo "📝 Next steps:"
    echo "1. Open app/src/main/java/com/example/jetpacktest/ml/ImageClassifier.kt"
    echo "2. Change this line:"
    echo "   private const val MODEL_FILE = \"$MODEL_NAME\""
    echo ""
    echo "3. Rebuild and run your app"
    echo ""
    echo "🎉 Your app will now use the better model!"
else
    echo ""
    echo "❌ Download failed. Please check your internet connection."
    exit 1
fi
