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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager (pulled in transitively by play-services-ads) instantiates its Room-
# generated WorkDatabase_Impl reflectively (Class.forName + newInstance) at startup via
# androidx.startup.InitializationProvider. Nothing calls that constructor directly, so R8
# strips it as unused - confirmed crash-on-open via device logcat on
# beautiful_asian_girl_pics_2: "NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init> []".
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
