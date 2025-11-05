# Add project specific ProGuard rules here.
-keep class org.tensorflow.** { *; }
-keep class com.gallery.categorized.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
