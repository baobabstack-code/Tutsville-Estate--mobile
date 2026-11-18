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

# Preserve line numbers and source file attributes so Play Console mapping.txt retraces crashes cleanly.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations and Kotlin metadata needed by Compose/Firebase/Credentials.
-keepattributes *Annotation*

# Preserve Enum values() and valueOf() when referenced reflectively.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Firestore-persisted model enum constant names for valueOf() and .name.
-keep enum com.example.tutsville.data.model.** { *; }
