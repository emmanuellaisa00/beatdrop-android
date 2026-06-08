# BeatDrop ProGuard rules

# Keep @JavascriptInterface methods for the WebView extractor
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# Mozilla Rhino
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
