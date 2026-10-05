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

# Keep line numbers in stack traces (deobfuscate crashes via the uploaded mapping file), but hide
# the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Shizuku instantiates the user service by class name in another process, so nothing in this app
# references its constructor.
-keep class dev.koukeneko.essentialkeytools.unlock.PackageToggleService {
    <init>();
}
