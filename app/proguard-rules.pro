# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Room ProGuard Rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Glance AppWidget Rules
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.action.ActionCallback
-keep class com.example.widget.** { *; }

# Keep line numbers for crash reports; keep app entry points and WorkManager workers
-keepattributes SourceFile,LineNumberTable,*Annotation*
-keep class com.example.QuranApp { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
