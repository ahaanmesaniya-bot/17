# ---- R7 Play release rules ----
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# App code
-keep class com.r7studio.r7Playapp.** { *; }

# WebView clients are called by the system
-keepclassmembers class * extends android.webkit.WebViewClient { public *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { public *; }

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Coil + OkHttp + Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.internal.platform.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin / coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn org.slf4j.**

# AdMob JS bridge (called from the bundled HTML games)
-keepclassmembers class com.r7studio.r7Playapp.AdBridge { @android.webkit.JavascriptInterface <methods>; }
-dontwarn com.google.android.gms.**
